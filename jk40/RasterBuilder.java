package jk40;

import java.util.Iterator;

/**
 * This class works as an iterable iterator based on certain states It's mapped
 * around the idea of a state machine and allocates nearly no memory in the
 * creation of rasters. It reuses a static int[3] block that provides:
 *
 * x, y, pixel
 *
 */
public class RasterBuilder implements Iterable<int[]>, Iterator<int[]> {

    public static final int X_AXIS = 0;
    public static final int TOP = 0;
    public static final int LEFT = 0;
    public static final int BIDIRECTIONAL = 0;
    public static final int SKIPPING = 0;
    public static final int Y_AXIS = 1;
    public static final int BOTTOM = 2;
    public static final int RIGHT = 4;
    public static final int UNIDIRECTIONAL = 8;
    public static final int NO_SKIP = 16;

    public static final int SERPENTINE_TRANVERSE_X_FROM_TOP_LEFT_SKIPPING_BLANK_LINES = X_AXIS | TOP | LEFT | BIDIRECTIONAL | SKIPPING;

    static final int COMMAND_UNCALCULATED = 0;
    static final int COMMAND_VALUE = 1;
    static final int COMMAND_FINISHED = 0xFF;

    private static final int STATE_NOT_INITIALIZED = 0;
    private static final int STATE_MOVED_TO_START = 1;
    private static final int STATE_PASS_ENDED = 2;

    private static final int STATE_MOVING_RIGHT = 10;
    private static final int STATE_MOVING_LEFT = 11;
    private static final int STATE_MOVING_TOP = 12;
    private static final int STATE_MOVING_BOTTOM = 13;

    private static final int STATE_LINESTEP_RIGHT = 20;
    private static final int STATE_LINESTEP_LEFT = 21;
    private static final int STATE_LINESTEP_TOP = 22;
    private static final int STATE_LINESTEP_BOTTOM = 23;

    public static final int CMD_MOVE = 0;
    public static final int CMD_CUT = 1;

    int state = STATE_NOT_INITIALIZED;
    int command_status = COMMAND_UNCALCULATED;

    RasterElement image;
    int transversal = SERPENTINE_TRANVERSE_X_FROM_TOP_LEFT_SKIPPING_BLANK_LINES;
    int skip_pixel_value;

    private int dy, dx, begin, end, higher_bound, lower_bound, pixel;
    private int overscan;
    final int[] pos = new int[4];

    public RasterBuilder(RasterElement image, int transversal, int skipvalue, int overscan) {
        this.image = image;
        this.transversal = transversal;
        this.skip_pixel_value = skipvalue;
        this.overscan = overscan;
    }

    @Override
    public Iterator<int[]> iterator() {
        return this;
    }

    public void reset() {
        command_status = STATE_NOT_INITIALIZED;
    }

    @Override
    public boolean hasNext() {
        // call calculate until it produces a command status.
        while (command_status == COMMAND_UNCALCULATED) {
            calculate();
        }
        return command_status != COMMAND_FINISHED;
    }

    @Override
    public int[] next() {
        if (command_status == COMMAND_FINISHED) {
            return null;
        }

        if (command_status == COMMAND_VALUE) {
            command_status = COMMAND_UNCALCULATED;
            return pos;
        }
        return null;
    }

    private void calculate() {
        switch (state) {
            case STATE_NOT_INITIALIZED:
                moveToStart();
                break;
            case STATE_MOVED_TO_START:
                initializeFirstLine();
                break;

            case STATE_MOVING_LEFT:
                moveLeft();
                break;
            case STATE_MOVING_TOP:
                moveTop();
                break;
            case STATE_MOVING_RIGHT:
                moveRight();
                break;
            case STATE_MOVING_BOTTOM:
                moveBottom();
                break;

            case STATE_LINESTEP_LEFT:
                linestepAtLeftEdge();
                break;
            case STATE_LINESTEP_TOP:
                linestepAtTopEdge();
                break;
            case STATE_LINESTEP_RIGHT:
                linestepAtRightEdge();
                break;
            case STATE_LINESTEP_BOTTOM:
                linestepAtBottomEdge();
                break;

            case STATE_PASS_ENDED:
                passEnded();
                break;
        }
    }

    private void passEnded() {
        //we only perform 1 pass.
        command_status = COMMAND_FINISHED;
    }

    private void moveToStart() {
        if ((transversal & RIGHT) != 0) {
            pos[0] = image.getWidth() - 1;
            dx = -1;
        } else {
            pos[0] = 0;
            dx = 1;
        }

        if ((transversal & BOTTOM) != 0) {
            pos[1] = image.getHeight() - 1;
            dy = -1;
        } else {
            pos[1] = 0;
            dy = 1;
        }
        pos[3] = CMD_MOVE;
        state = STATE_MOVED_TO_START;
    }

    private void initializeFirstLine() {
        if ((transversal & Y_AXIS) != 0) {
            if ((transversal & BOTTOM) != 0) {
                initGoingTop();
            } else {
                initGoingBottom();
            }
        } else {
            if ((transversal & RIGHT) != 0) {
                initGoingLeft();
            } else {
                initGoingRight();
            }
        }
    }

    private boolean checkPassFinishedXAxis() {
        if (!inrange_y(pos[1])) {
            state = STATE_PASS_ENDED;
            return true;
        }
        return false;
    }

    private boolean checkPassFinishedYAxis() {
        if (!inrange_x(pos[0])) {
            state = STATE_PASS_ENDED;
            return true;
        }
        return false;
    }

    private boolean initializeXAxisLine() {
        lower_bound = leftMostNotEqual(pos[1], skip_pixel_value);
        if ((lower_bound == -1) && ((transversal & NO_SKIP) == 0)) {
            //This is a blank line. Keep stepping.
            pos[1] += dy;
            return true;
        }
        higher_bound = rightMostNotEqual(pos[1], skip_pixel_value);
        return false;
    }

    private boolean initializeYAxisLine() {
        lower_bound = topMostNotEqual(pos[0], skip_pixel_value);
        if ((lower_bound == -1) && ((transversal & NO_SKIP) == 0)) {
            //This is a blank line. Keep stepping.
            pos[0] += dx;
            return true;
        }
        higher_bound = bottomMostNotEqual(pos[0], skip_pixel_value);
        return false;
    }

    private void initGoingLeft() {
        if (checkPassFinishedXAxis()) {
            return;
        }
        if (initializeXAxisLine()) {
            return;
        }

        end = lower_bound - overscan;
        begin = higher_bound + overscan;

        if (inrange_y(pos[1] + dy)) {
            //If the next line in the dy direction also has a further end. We stop there.
            end = Math.min(end, leftMostNotEqual(pos[1] + dy, skip_pixel_value));
        }

        command_status = COMMAND_VALUE;
        pos[3] = CMD_MOVE;
        state = STATE_MOVING_LEFT;
    }

    private void initGoingTop() {
        if (checkPassFinishedYAxis()) {
            return;
        }

        if (initializeYAxisLine()) {
            return;
        }

        end = lower_bound - overscan;
        begin = higher_bound + overscan;

        if (inrange_x(pos[0] + dx)) {
            //If the next line in the dx direction also has a further end. We stop there.
            end = Math.min(end, topMostNotEqual(pos[0] + dx, skip_pixel_value));
        }

        command_status = COMMAND_VALUE;
        pos[3] = CMD_MOVE;
        state = STATE_MOVING_TOP;
    }

    private void initGoingRight() {
        if (checkPassFinishedXAxis()) {
            return;
        }

        if (initializeXAxisLine()) {
            return;
        }

        begin = lower_bound - overscan;
        end = higher_bound + overscan;

        if (inrange_y(pos[1] + dy)) {
            //If the next line in the dy direction also has a further end. We stop there.
            end = Math.max(end, rightMostNotEqual(pos[1] + dy, skip_pixel_value));
        }

        command_status = COMMAND_VALUE;
        pos[3] = CMD_MOVE;
        state = STATE_MOVING_RIGHT;
    }

    private void initGoingBottom() {
        if (checkPassFinishedYAxis()) {
            return;
        }

        if (initializeYAxisLine()) {
            return;
        }

        begin = lower_bound - overscan;
        end = higher_bound + overscan;

        if (inrange_x(pos[0] + dx)) {
            //If the next line in the dx direction also has a further end. We stop there.
            end = Math.max(end, bottomMostNotEqual(pos[0] + dx, skip_pixel_value));
        }

        command_status = COMMAND_VALUE;
        pos[3] = CMD_MOVE;
        state = STATE_MOVING_BOTTOM;
    }

    private void linestepAtLeftEdge() {
        pos[1] += dy;
        initGoingRight();
    }

    private void linestepAtTopEdge() {
        pos[0] += dx;
        initGoingBottom();
    }

    private void linestepAtRightEdge() {
        pos[1] += dy;
        initGoingLeft();
    }

    private void linestepAtBottomEdge() {
        pos[0] += dx;
        initGoingTop();
    }

    private void getUpdatedPixelAtLocation() {
        pixel = skip_pixel_value;
        if (inrange_x(pos[0]) && inrange_y(pos[1])) {
            pixel = image.getPixel(pos[0], pos[1]);
        }
        pos[2] = pixel;
        command_status = COMMAND_VALUE;
    }

    private void commitPosition() {
        if (pixel == skip_pixel_value) {
            pos[3] = CMD_MOVE;
        } else {
            pos[3] = CMD_CUT;
        }
    }

    private void moveLeft() {
        getUpdatedPixelAtLocation();
        pos[0] = nextColorChangeHeadingLeft(pos[0], pos[1], end);
        pos[0] = Math.max(pos[0], end);
        commitPosition();

        if (end < pos[0]) {
            state = STATE_MOVING_LEFT;
        } else {
            state = STATE_LINESTEP_LEFT;
        }
    }

    private void moveTop() {
        getUpdatedPixelAtLocation();
        pos[1] = nextColorChangeHeadingTop(pos[0], pos[1], end);
        pos[1] = Math.max(pos[1], end);
        commitPosition();

        if (end < pos[1]) {
            state = STATE_MOVING_TOP;
        } else {
            state = STATE_LINESTEP_TOP;
        }
    }

    private void moveRight() {
        getUpdatedPixelAtLocation();
        pos[0] = nextColorChangeHeadingRight(pos[0], pos[1], end);
        pos[0] = Math.min(pos[0], end);
        commitPosition();

        if (pos[0] < end) {
            state = STATE_MOVING_RIGHT;
        } else {
            state = STATE_LINESTEP_RIGHT;
        }
    }

    private void moveBottom() {
        getUpdatedPixelAtLocation();
        pos[1] = nextColorChangeHeadingBottom(pos[0], pos[1], end);
        pos[1] = Math.min(pos[1], end);
        commitPosition();

        if (pos[1] < end) {
            state = STATE_MOVING_BOTTOM;
        } else {
            state = STATE_LINESTEP_BOTTOM;
        }
    }

    private boolean inrange_y(int y) {
        return y < image.getHeight() && y >= 0;
    }

    private boolean inrange_x(int x) {
        return x < image.getWidth() && x >= 0;
    }

    /**
     * Finds the x coordinate for the left most pixel, since "start" depends on
     * what direction you are cutting in.
     *
     * @param y
     * @param v, seek value
     * @return x coordinate of left most non-matching pixel
     */
    protected int leftMostNotEqual(int y, int v) {
        for (int x = 0; x < image.getWidth(); x++) {
            int px = image.getPixel(x, y);
            if (px != v) {
                return x;
            }
        }
        return -1;
    }

    /**
     * Finds the y coordinate for the top most pixel, since "start" depends on
     * what direction you are cutting in.
     *
     * @param x
     * @param v, seek value
     * @return y coordinate of top most non-matching pixel
     */
    protected int topMostNotEqual(int x, int v) {
        for (int y = 0; y < image.getHeight(); y++) {
            int px = image.getPixel(x, y);
            if (px != v) {
                return y;
            }
        }
        return -1;
    }

    /**
     * Finds the x coordinate for the right most pixel, since "end" depends on
     * what direction you are cutting in.
     *
     * @param y, scanline to check
     * @param v, seek value
     * @return x coordinate of right most non-matching pixel
     */
    protected int rightMostNotEqual(int y, int v) {
        for (int x = image.getWidth() - 1; x >= 0; x--) {
            int px = image.getPixel(x, y);
            if (px != v) {
                return x;
            }
        }
        return image.getWidth();
    }

    /**
     * Finds the y coordinate for the bottom most pixel, since "end" depends on
     * what direction you are cutting in.
     *
     * @param x, scanline to check
     * @param v, seek value
     * @return y coordinate of bottom most non-matching pixel
     */
    protected int bottomMostNotEqual(int x, int v) {
        for (int y = image.getHeight() - 1; y >= 0; y--) {
            int px = image.getPixel(x, y);
            if (px != v) {
                return y;
            }
        }
        return image.getHeight();
    }

    /**
     * nextColorChange logic when heading <-
     *
     * @param x x coordinate to start scanning from
     * @param y y coordinate to start scanning from
     * @param def, default value to return if there are no changes.
     * @return x coordinate of the next different color in this row
     */
    public int nextColorChangeHeadingLeft(int x, int y, int def) {
        if (x <= -1) {
            return def;
        }
        if (x == 0) {
            return -1;
        }
        if (x == image.getWidth()) {
            return image.getWidth() - 1;
        }
        if (image.getWidth() < x) {
            return image.getWidth();
        }

        int v = image.getPixel(x, y);
        for (int ix = x; ix >= 0; ix--) {
            int px = image.getPixel(ix, y);
            if (px != v) {
                return ix;
            }
        }
        return 0;
    }

    /**
     * nextColorChange logic when heading <-
     *
     * @param x x coordinate to start scanning from
     * @param y y coordinate to start scanning from
     * @param def, default value to return if there are no changes.
     * @return x coordinate of the next different color in this row
     */
    public int nextColorChangeHeadingTop(int x, int y, int def) {

        if (y <= -1) {
            return def;
        }
        if (y == 0) {
            return -1;
        }
        if (y == image.getHeight()) {
            return image.getHeight() - 1;
        }
        if (image.getHeight() < y) {
            return image.getHeight();
        }

        int v = image.getPixel(x, y);
        for (int iy = y; iy >= 0; iy--) {
            int px = image.getPixel(x, iy);
            if (px != v) {
                return iy;
            }
        }
        return 0;
    }

    /**
     * nextColorChange logic when heading ->
     *
     * @param x x coordinate to start scanning from
     * @param y y coordinate to start scanning from
     * @param def, default value to return if there are no changes.
     * @return x coordinate of the next different color in this row
     */
    public int nextColorChangeHeadingRight(int x, int y, int def) {
        if (x < -1) {
            return -1;
        }
        if (x == -1) {
            return 0;
        }
        if (x == image.getWidth() - 1) {
            return image.getWidth();
        }
        if (image.getWidth() <= x) {
            return def;
        }

        int v = image.getPixel(x, y);
        for (int ix = x; ix < image.getWidth(); ix++) {
            int px = image.getPixel(ix, y);
            if (px != v) {
                return ix;
            }
        }
        return image.getWidth() - 1;
    }

    /**
     * nextColorChange logic when heading ->
     *
     * @param x x coordinate to start scanning from
     * @param y y coordinate to start scanning from
     * @param def, default value to return if there are no changes.
     * @return x coordinate of the next different color in this row
     */
    public int nextColorChangeHeadingBottom(int x, int y, int def) {
        if (y < -1) {
            return -1;
        }
        if (y == -1) {
            return 0;
        }
        if (y == image.getHeight() - 1) {
            return image.getHeight();
        }
        if (image.getHeight() <= y) {
            return def;
        }

        int v = image.getPixel(x, y);
        for (int iy = y; iy < image.getHeight(); iy++) {
            int px = image.getPixel(x, iy);
            if (px != v) {
                return iy;
            }
        }
        return image.getHeight() - 1;
    }
}
