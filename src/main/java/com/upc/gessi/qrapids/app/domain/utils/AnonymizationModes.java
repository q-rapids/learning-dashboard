package com.upc.gessi.qrapids.app.domain.utils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AnonymizationModes {

    CAPITALS("Capitals", Capitals.class),
    COUNTRIES("Countries", Countries.class),
    GREEK_ALPHABET("Greek Alphabet", GreekAlphabet.class);

    private final String modeName;
    private final Class<? extends Enum<?>> mode;

    <T extends Enum<T>> AnonymizationModes(String modeName, Class<T> mode){
        this.modeName = modeName;
        this.mode = mode;
    }

    @JsonValue
    public String getModeName() {
        return modeName;
    }

    public Class<? extends Enum<?>> getMode() {
        return mode;
    }

    public Enum<?>[] getValues(){
        return mode.getEnumConstants();
    }


    @JsonCreator
    public static AnonymizationModes fromString(String anonymizationMode){
        return anonymizationMode == null ? null : AnonymizationModes.valueOf(anonymizationMode.toUpperCase());
    }

}
