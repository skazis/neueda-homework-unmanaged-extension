package com.neo4j.homework.unmanagedextension.domain;

/**
 * Gender enum.
 */
public enum Gender {

    MALE("male", "m"),
    FEMALE("female", "f");

    private String longDescription;
    private String shortDescription;

    Gender(String longDescription, String shortDescription) {
        this.longDescription = longDescription;
        this.shortDescription = shortDescription;
    }

    public String getLongDescription() {
        return longDescription;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    /**
     * Validates given gender string. Must be one of the short or long descriptions.
     */
    public static boolean isGender(final String gender) {
        if (gender == null || gender.isEmpty()) {
            return false;
        }

        for (Gender genderEnum : Gender.values()) {
            if (gender.equals(genderEnum.getLongDescription()) || gender.equals(genderEnum.getShortDescription())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets long form of the gender.
     */
    public static String getLongForm(final String gender) {
        if (isGender(gender)) {
            for (Gender genderEnum : Gender.values()) {
                if (gender.equals(genderEnum.getLongDescription()) || gender.equals(genderEnum.getShortDescription())) {
                    return genderEnum.getLongDescription();
                }
            }
        }
        throw new IllegalArgumentException(String.format("Gender's value [%s] is not defined.", gender));
    }
}
