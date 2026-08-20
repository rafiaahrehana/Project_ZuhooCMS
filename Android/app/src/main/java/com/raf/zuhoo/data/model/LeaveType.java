package com.raf.zuhoo.data.model;

// Mirrors com.zuhoo.enums.LeaveType on the backend. A fixed, small set of wire constants — unlike
// service categories, this never varies per company, so the create-request screen builds its
// spinner from this array rather than an API call (same reasoning as the priority spinner).
public final class LeaveType {

    public static final String ANNUAL = "ANNUAL";
    public static final String SICK = "SICK";
    public static final String CASUAL = "CASUAL";
    public static final String MATERNITY = "MATERNITY";
    public static final String PATERNITY = "PATERNITY";
    public static final String UNPAID = "UNPAID";
    public static final String COMPENSATORY = "COMPENSATORY";
    public static final String EMERGENCY = "EMERGENCY";
    public static final String OTHER = "OTHER";

    public static final String[] VALUES = {
            ANNUAL, SICK, CASUAL, MATERNITY, PATERNITY, UNPAID, COMPENSATORY, EMERGENCY, OTHER
    };

    private LeaveType() {
    }
}
