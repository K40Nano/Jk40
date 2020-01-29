package jk40;

/**
 *
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

    K40Queue queue;
    private StringBuilder builder = new StringBuilder();

    private int mode = UNINIT;

    private boolean is_top = false;
    private boolean is_left = false;
    private boolean is_on = false;

    private String board = "M2";
    private double speed = 30;
    private int raster_step = 1;
    private int power = 1000;
    double d_ratio = 0.2612;

    private int power_remainder = 0;
    private int x = 0;
    private int y = 0;

    void open() {
        open(new K40Queue());
    }

    void open(K40Queue q) {
        queue = q;
        q.open();
    }

    void close() {
        if (mode == COMPACT) {
            exit_compact_mode();
            execute();
        }
        queue.close();
        queue = null;
    }

    public String getBoard() {
        return board;
    }

    public void setBoard(String board) {
        this.board = board;
    }

    void setSpeed(double mm_per_second) {
        if (mode == COMPACT) {
            exit_compact_mode();
        }
        speed = mm_per_second;
    }

    void setPower(int ppi) {
        power = ppi;
    }

    public int getRaster_step() {
        return raster_step;
    }

    public void setRaster_step(int raster_step) {
        if (raster_step > 64) {
            raster_step = 64;
        }
        if (raster_step < 1) {
            raster_step = 1;
        }
        this.raster_step = raster_step;
    }

    public double getD_ratio() {
        return d_ratio;
    }

    public void setD_ratio(double d_ratio) {
        this.d_ratio = d_ratio;
    }

    void send() {
        queue.add(builder.toString());
        builder.delete(0, builder.length());
    }

    void home() {
        exit_compact_mode();
        builder.append("IPP\n");
        send();
        mode = UNINIT;
        x = 0;
        y = 0;
    }

    void unlock_rail() {
        exit_compact_mode();
        builder.append("IS2P\n");
        send();
        mode = UNINIT;
    }

    void lock_rail() {
        exit_compact_mode();
        builder.append("IS1P\n");
        send();
        mode = UNINIT;
    }

    void move_absolute(int x, int y) {
        int dx = x - this.x;
        int dy = y - this.y;
        move_relative(dx, dy);
    }

    void move_relative(int dx, int dy) {
        if ((dx == 0) && (dy == 0)) {
            return;
        }
        check_init();
        laser_off();
        if (mode == DEFAULT) {
            encode_default_move(dx, dy);
        } else {
            makeLine(0, 0, dx, dy);
        }
        send();
    }

    void cut_absolute(int x, int y) {

        int dx = x - this.x;
        int dy = y - this.y;
        cut_relative(dx, dy);
    }

    void cut_relative(int dx, int dy) {
        if ((dx == 0) && (dy == 0)) {
            return;
        }
        check_init();
        if (mode != COMPACT) {
            start_compact_mode();
        }
        laser_on();
        makeLine(0, 0, dx, dy);
        send();
    }

    void raster_start() {
        laser_off();
        check_init();
        if (mode == COMPACT) {
            exit_compact_mode();
        }
        builder.append(getSpeed(speed, true));
        builder.append('N');
        builder.append(BOTTOM);
        builder.append(RIGHT);
        builder.append("S1E");
        is_top = false;
        is_left = false;
        mode = COMPACT;
    }

    void raster_end() {
        exit_compact_mode();
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
        }
        mode = COMPACT;
    }

    void execute() {
        queue.execute();
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

    void move_x(int dx) {
        if (0 < dx) {
            builder.append(RIGHT);
            is_left = false;
        } else {
            builder.append(LEFT);
            is_left = true;
        }
        distance(Math.abs(dx));
        this.x += dx;
    }

    void move_y(int dy) {
        if (0 < dy) {
            builder.append(BOTTOM);
            is_top = false;
        } else {
            builder.append(TOP);
            is_top = true;
        }
        distance(Math.abs(dy));
        this.y += dy;
    }

    void move_angle(int dx, int dy) {
        //assert(abs(dx) == abs(dy));
        if (0 < dx) {
            if (is_left) {
                builder.append(RIGHT);
            }
            is_left = false;
        } else {
            if (!is_left) {
                builder.append(LEFT);
            }
            is_left = true;
        }
        if (0 < dy) {
            if (is_top) {
                builder.append(BOTTOM);
            }
            is_top = false;
        } else {
            if (!is_top) {
                builder.append(TOP);
            }
            is_top = true;
        }
        builder.append(DIAGONAL);
        distance(Math.abs(dx));
        this.x += dx;
        this.y += dy;
    }

    void move_diagonal(int v) {
        builder.append(DIAGONAL);
        distance(Math.abs(v));
        if (is_top) {
            this.y -= v;
        } else {
            this.y += v;
        }
        if (is_left) {
            this.x -= v;
        } else {
            this.x += v;
        }
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
        is_on = false;
    }

    public void h_switch() {
        if (is_left) {
            this.set_right();
        } else {
            this.set_left();
        }
    }

    public void v_switch() {
        if (is_top) {
            this.set_bottom();
        } else {
            this.set_top();
        }
    }

    /*
    * Zingl-Bresenham line draw algorithm
    * With Tatarize's PPI carryforward power modulation.
    * 
    * The general goal of this is to trigger a state sync, if the laser is to
    * change state or if the next movement is not exactly diagonal or orthogonal
    * all other states can be combined into the same movement.
     */
    void makeLine(int x0, int y0, int x1, int y1) {
        int dx = Math.abs(x1 - x0);
        int dy = -Math.abs(y1 - y0);
        int sx = (x0 < x1) ? 1 : -1;
        int sy = (y0 < y1) ? 1 : -1;

        int err = dx + dy;  //error value e_xy
        int cud_x = 0; //Current unapplied delta x
        int cud_y = 0; //Current unapplied delta y
        int pud_x = 0; //Previous unapplied delta x
        int pud_y = 0; //Previous unapplied delta y
        boolean laser_cutting = this.is_on;
        boolean pulse_on = this.is_on;

        while (true) {
            /* loop */

            if (laser_cutting) {
                power_remainder += power;
                if (power_remainder >= 1000) {
                    power_remainder -= 1000;
                    pulse_on = true;
                } else {
                    pulse_on = false;
                }
            }
            int abs_cud_x = Math.abs(cud_x);
            int abs_cud_y = Math.abs(cud_y);

            if ((this.is_on != pulse_on)
                    || ((abs_cud_x != abs_cud_y)
                    && (abs_cud_x != 0)
                    && (abs_cud_y != 0))) {
                // The current settings do not combine. Actualize previous values.
                if (Math.abs(pud_x) == Math.abs(pud_y)) {
                    move_angle(pud_x, pud_y);
                } else if ((pud_y == 0) && (pud_x != 0)) {
                    move_x(pud_x);
                } else if ((pud_y != 0) && (cud_y == 0)) {
                    move_y(cud_y);
                }
                else {
                    throw new IllegalStateException("This state should be impossible.");
                }
                cud_x -= pud_x; //The difference of the values *is* combinable.
                cud_y -= pud_y;
                if (pulse_on) {
                    laser_on(); //set laser to the correct state.
                } else {
                    laser_off();
                }
            }
            //yield x0, y0
            if ((x0 == x1) && (y0 == y1)) {
                //update final values.
                break;
            }
            int e2 = 2 * err;
            if (e2 >= dy) {//  # e_xy+e_y < 0
                err += dy;
                pud_x = cud_x;
                x0 += sx;
                cud_x += sx;

            }
            if (e2 <= dx) {//  # e_xy+e_y < 0
                err += dx;
                pud_y = cud_y;
                y0 += sy;
                cud_y += sy;
            }
        }
    }

    public void distance(int v) {
        if (v >= 255) {
            int z_count = v / 255;
            v %= 255;
            for (int i = 0; i < z_count; i++) {
                builder.append("z");
            }
        }
        if (v > 51) {
            builder.append(String.format("%03d", v));
            return;
        } else if (v > 25) {
            builder.append('|');
            v -= 25;
        }
        if (v > 0) {
            builder.append((char) ('a' + (v - 1)));
        }
    }

    public int getGear(double mm_per_second) {
        if (mm_per_second < 7) {
            return 0;
        }
        if (mm_per_second < 25.4) {
            return 1;
        }
        if (mm_per_second < 60) {
            return 2;
        }
        if (mm_per_second < 127) {
            return 3;
        }
        return 4;
    }

    public int getGearRaster(double mm_per_second) {
        if (mm_per_second < 25.4) {
            return 1;
        }
        if (mm_per_second < 127) {
            return 2;
        }
        if (mm_per_second < 320) {
            return 3;
        }
        return 4;
    }

    public String getSpeed(double mm_per_second) {
        return getSpeed(mm_per_second, false);
    }

    public String getSpeed(double mm_per_second, boolean raster) {
        int gear;
        if (raster) {
            if (mm_per_second > 500) {
                mm_per_second = 500;
            }
            gear = getGearRaster(mm_per_second);
        } else {
            if (mm_per_second > 240) {
                mm_per_second = 240;
            }
            gear = getGear(mm_per_second);
        }
        double b;
        double m = 11148.0;
        if ("M2".equals(board)) {
            switch (gear) {
                case 0:
                    b = 8;
                    m = 929.0;
                    break;
                default:
                    b = 5120.0;
                    break;
                case 3:
                    b = 5632.0;
                    break;
                case 4:
                    b = 6144.0;
                    break;
            }
            return getSpeed(mm_per_second, m, b, gear, true, raster);
        }
        if ("M".equals(board) || "M1".equals(board)) {
            if (gear == 0) {
                gear = 1;
            }
            m = 11148.0;
            switch (gear) {
                default:
                    b = 5120.0;
                    break;
                case 3:
                    b = 5632.0;
                    break;
                case 4:
                    b = 6144.0;
                    break;
            }
            return getSpeed(mm_per_second, m, b, gear, "M1".equals(board), raster);
        }
        if ("A".equals(board) || "B".equals(board) || "B1".equals(board)) {
            if (gear == 0) {
                gear = 1;
            }
            m = 11148.0;
            switch (gear) {
                default:
                    b = 5120.0;
                    break;
                case 3:
                    b = 5632.0;
                    break;
                case 4:
                    b = 6144.0;
                    break;
            }
            return getSpeed(mm_per_second, m, b, gear, true, raster);
        }
        if ("B2".equals(board)) {
            m = 22296.0;
            switch (gear) {
                case 0:
                    b = 784.0;
                    m = 1858.0;
                    break;
                default:
                    b = 784.0;
                    break;
                case 3:
                    b = 896.0;
                    break;
                case 4:
                    b = 1024.0;
                    break;
            }
            return getSpeed(mm_per_second, m, b, gear, true, raster);
        }
        throw new UnsupportedOperationException("Board is not known.");
    }

    String getSpeed(double mm_per_second, double m, double b, int gear, boolean diagonal_code_required, boolean raster) {
        boolean suffix_c = false;
        if (gear == 0) {
            gear = 1;
            suffix_c = true;
        }
        double frequency_kHz = mm_per_second / 25.4;
        double period_in_ms = 1.0 / frequency_kHz;
        double period_value = (m * period_in_ms) + b;
        int speed_value = 65536 - (int) Math.rint(period_value);
        if (speed_value < 0) {
            speed_value = 0;
        }
        if (speed_value > 65535) {
            speed_value = 65535;
        }
        if (raster) {
            return String.format(
                    "V%03d%03d%1dG%03d",
                    (speed_value >> 8) & 0xFF, (speed_value & 0xFF),
                    gear, raster_step);
        }
        if (!diagonal_code_required) {
            if (suffix_c) {
                return String.format(
                        "CV%03d%03d%1dC",
                        (speed_value >> 8) & 0xFF, (speed_value & 0xFF),
                        gear);
            } else {
                return String.format(
                        "CV%03d%03d%1d",
                        (speed_value >> 8) & 0xFF, (speed_value & 0xFF),
                        gear);
            }
        }
        int step_value = (int) mm_per_second;
        double d_value = d_ratio * (m * period_in_ms) / (double) step_value;
        int diag_add = (int) d_value;
        if (diag_add < 0) {
            diag_add = 0;
        }
        if (diag_add > 65535) {
            diag_add = 65535;
        }
        if (suffix_c) {
            return String.format(
                    "CV%03d%03d%1d%03d%03d%03dC",
                    (speed_value >> 8) & 0xFF, (speed_value & 0xFF),
                    gear,
                    step_value,
                    (diag_add >> 8) & 0xFF, (diag_add & 0xFF));
        }
        return String.format(
                "CV%03d%03d%1d%03d%03d%03d",
                (speed_value >> 8) & 0xFF, (speed_value & 0xFF),
                gear,
                step_value,
                (diag_add >> 8) & 0xFF, (diag_add & 0xFF));
    }

}
