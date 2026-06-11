package com.astral.express.pccms.room.security;

public final class RoomPermissions {

    private RoomPermissions() {
    }

    public static final String ROOM_MANAGE = "hasAuthority('ROOM_MANAGE')";
    public static final String ROOM_READ =
            "hasAuthority('ROOM_MANAGE') or hasAuthority('BOARDING_READ') "
                    + "or hasAuthority('BOARDING_CREATE') or hasAuthority('BOARDING_UPDATE')";
}
