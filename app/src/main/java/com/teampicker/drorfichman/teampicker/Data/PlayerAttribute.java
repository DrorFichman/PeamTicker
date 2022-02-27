package com.teampicker.drorfichman.teampicker.Data;

public enum PlayerAttribute {
    isGK("_is_gk", "GK"),
    isDefender("_is_defender", "D"),
    isPlaymaker("_is_playmaker", "PM"),
    isUnbreakable("_is_breakable", "B"),
    isExtra("_is_extra", "E");

    public String attribute;
    public String displayName;

    PlayerAttribute(String att, String display) {
        attribute = att;
        displayName = display;
    }
}

