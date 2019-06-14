/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jk40;

/**
 *
 * @author Tat
 */
public class K40Device {

    private static final int UNINIT = 0;
    private static final int DEFAULT = 1;
    private static final int COMPACT = 2;

    static final char LASER_ON = 'D';
    static final char LASER_OFF = 'U';

    static final char RIGHT = 'B';
    static final char LEFT = 'T';
    static final char TOP = 'L';
    static final char BOTTOM = 'R';
    static final char DIAGONAL = 'M';

    K40Queue jobber;
    private StringBuilder builder = new StringBuilder();

    private int mode = UNINIT;

    private boolean is_top = false;
    private boolean is_left = false;
    private boolean is_on = false;

    private double speed = 30;
    private double power = 1;
    private String board = "M2";

    private int x = 0;
    private int y = 0;

    void open() {
        jobber = new K40Queue();
        jobber.open();
    }

    void close() {
        jobber.close();
        jobber = null;
    }

    void setPower(double power) {
        this.power = power;
    }

    void setSpeed(double mm_per_second) {
        if (mode == COMPACT) {
            exit_compact_mode();
        }
        speed = mm_per_second;
    }

    public String getBoard() {
        return board;
    }

    public void setBoard(String board) {
        this.board = board;
    }

    void send() {
        jobber.add(builder.toString());
        builder.delete(0, builder.length());
    }

    void move_absolute(int x, int y) {
        int dx = x - this.x;
        int dy = y - this.y;
        move_relative(dx, dy);
    }

    void move_relative(int dx, int dy) {
        if (mode != DEFAULT) {
            exit_compact_mode();
        }
        check_init();
        this.x += dx;
        this.y += dy;
        encode_default_move(dx, dy);
        send();
    }

    void cut_absolute(int x, int y) {
        int dx = x - this.x;
        int dy = y - this.y;
        cut_relative(dx, dy);
    }

    void cut_relative(int dx, int dy) {
        check_init();
        if (mode != COMPACT) {
            start_compact_mode();
        }
        this.x += dx;
        this.y += dy;
        laser_on();
        makeLine(0, 0, dx, dy);
        send();
    }

    void check_init() { 
        if (mode == UNINIT) {
            builder.append('I');
            mode = DEFAULT;
        }
    }
    void exit_compact_mode() {
        if (mode == COMPACT) {
            builder.append("FNSE-\n");
            send();
            is_on = false;
            mode = UNINIT;
        }
    }

    void start_compact_mode() {
        check_init();
        if (mode == COMPACT) {
            return;
        }
        if (mode == DEFAULT) {
            encode_speed(speed);
            builder.append('N');
            if (is_top) {
                builder.append(TOP);
            } else {
                builder.append(BOTTOM);
            }
            if (is_left) {
                builder.append(LEFT);
            } else {
                builder.append(RIGHT);
            }
            builder.append("S1E");
            is_top = false;
            is_left = false;
        }
        mode = COMPACT;
    }

    void execute() {
        if (mode == COMPACT) {
            exit_compact_mode();
        }
        jobber.execute();
    }

    void encode_default_move(int dx, int dy) {
        move_x(dx);
        move_y(dy);
        builder.append("S1P\n");
        mode = UNINIT;
    }

    void encode_speed(double speed) {
        builder.append(getSpeed(speed));
    }

    void move_x(int x) {
        if (0 < x) {
            builder.append(RIGHT);
            is_left = false;
        } else {
            builder.append(LEFT);
            is_left = true;
        }
        distance(Math.abs(x));
    }

    void move_y(int y) {
        if (0 < y) {
            builder.append(BOTTOM);
            is_top = false;
        } else {
            builder.append(TOP);
            is_top = true;
        }
        distance(Math.abs(y));
    }

    void move_diagonal(int v) {
        builder.append(DIAGONAL);
        distance(Math.abs(v));
    }

    void set_top() {
        if (!is_top) {
            builder.append(TOP);
        }
        is_top = true;
    }

    void set_bottom() {
        if (is_top) {
            builder.append(BOTTOM);
        }
        is_top = false;
    }

    void set_left() {
        if (!is_left) {
            builder.append(LEFT);
        }
        is_left = true;
    }

    void set_right() {
        if (is_left) {
            builder.append(RIGHT);
        }
        is_left = false;
    }

    void laser_on() {
        if (!is_on) {
            builder.append(LASER_ON);
        }
        is_on = true;
    }

    void laser_off() {
        if (is_on) {
            builder.append(LASER_OFF);
        }
        is_on = true;
    }

    void makeLine(int x0, int y0, int x1, int y1) {
        int dy = y1 - y0; //BRESENHAM LINE DRAW ALGORITHM
        int dx = x1 - x0;

        int stepx, stepy;

        if (dy < 0) {
            dy = -dy;
            stepy = -1;
            set_top();
        } else {
            stepy = 1;
            set_bottom();
        }

        if (dx < 0) {
            dx = -dx;
            stepx = -1;
            set_left();
        } else {
            stepx = 1;
            set_right();
        }
        int straight = 0;
        int diagonal = 0;

        if (dx > dy) {
            dy <<= 1;                                                  // dy is now 2*dy
            dx <<= 1;
            int fraction = dy - (dx >> 1);                         // same as 2*dy - dx
            while (x0 != x1) {
                if (fraction >= 0) {
                    y0 += stepy;
                    fraction -= dx;                                // same as fraction -= 2*dx
                    if (straight != 0) {
                        move_x(straight);
                        straight = 0;
                    }
                    diagonal++;
                } else {
                    if (diagonal != 0) {
                        move_diagonal(diagonal);
                        diagonal = 0;
                    }
                    straight += stepx;
                }
                x0 += stepx;
                fraction += dy;                                    // same as fraction += 2*dy
            }
            if (straight != 0) {
                move_x(straight);
                straight = 0;
            }
        } else {
            dy <<= 1;                                                  // dy is now 2*dy
            dx <<= 1;                                                  // dx is now 2*dx
            int fraction = dx - (dy >> 1);
            while (y0 != y1) {
                if (fraction >= 0) {
                    x0 += stepx;
                    fraction -= dy;
                    if (straight != 0) {
                        move_y(straight);
                        straight = 0;
                    }
                    diagonal++;
                } else {
                    if (diagonal != 0) {
                        move_diagonal(diagonal);
                        diagonal = 0;
                    }
                    straight += stepy;
                }
                y0 += stepy;
                fraction += dx;
            }
            if (straight != 0) {
                move_y(straight);
                straight = 0;
            }
        }
        if (diagonal != 0) {
            move_diagonal(diagonal);
            diagonal = 0;
        }
    }

    //TODO: needs rechecking.
    public void distance(int v) {
        if (v >= 255) {
            int z_count = v / 255;
            v %= 255;
            for (int i = 0; i < z_count; i++) {
                builder.append("z");
            }
        }
        if (v > 53) {
            builder.append(String.format("%03d", v));
        } else if (v > 25) {
            builder.append('|').append((char) ('a' + (v - 26)));
        } else if (v > 0) {
            builder.append((char) ('a' + (v - 1)));
        }
    }

    //TODO: Expand this to support other boards and gearing codes. Only M2 currently.
    public String getSpeed(double mm_per_second) {
        mm_per_second = validateSpeed(mm_per_second);
        double b = 5120.0;
        double m = 11148.0;
        return getSpeed(mm_per_second, m, b, 1, true);
    }

    //TODO: Use suffix C mode for boards that support it.
    String getSpeed(double mm_per_second, double m, double b, int gear, boolean expanded) {
        double frequency_kHz = mm_per_second / 25.4;
        double period_in_ms = 1.0 / frequency_kHz;
        double period_value = (m * period_in_ms) + b;
        int speed_value = 65536 - (int) Math.rint(period_value);
        if (!expanded) {
            return String.format(
                    "CV%03d%03d%1d",
                    (speed_value >> 8) & 0xFF, (speed_value & 0xFF),
                    gear);
        }
        int step_value = (int) mm_per_second;
        double d_ratio = 0.2612;
        double d_value = d_ratio * (m * period_in_ms) / (double) step_value;
        int diag_add = (int) d_value;
        return String.format(
                "CV%03d%03d%1d%03d%03d%03d",
                (speed_value >> 8) & 0xFF, (speed_value & 0xFF),
                gear,
                step_value,
                (diag_add >> 8) & 0xFF, (diag_add & 0xFF));
    }

    //TODO: Validate the speeds.
    double validateSpeed(double s) {
        return s;
    }

}
