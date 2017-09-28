package com.mahmoud;

import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.ULocale;
import net.jcip.annotations.Immutable;

import java.time.*;
import java.util.GregorianCalendar;
import java.util.Objects;

/**
 * A persian date without time class, based on ICU4J library.
 *
 * <p>
 * {@code PersianDate} is an immutable date-time object that represents a date,
 * often viewed as year-month-day.
 *
 * <p>
 * This class is immutable and can be used in multi-threaded programs.
 * 
 * @author Mahmoud Fathi
 */
@Immutable
public final class PersianDate implements Comparable<PersianDate> {

    /**
     * Persian date.
     */
    private final Calendar persianDate;

    /**
     * Corresponding gregorian date. Since gregorian date is used frequently in the internal
     * implementation of this class, it is cached.
     */
    private final LocalDate gregDate;

    /**
     * @return the year
     */
    public int getYear() {
        return persianDate.get(Calendar.YEAR);
    }

    /**
     * @return the month-of-year field using the {@code Month} enum.
     * @see #getMonthValue()
     */
    public Month getMonth() {
        return Month.of(persianDate.get(Calendar.MONTH) + 1);
    }

    /**
     * @return the month-of-year, from 1 to 12
     * @see #getMonth()
     */
    public int getMonthValue() {
        return persianDate.get(Calendar.MONTH) + 1;
    }

    /**
     * @return day-of-month, from 1 to 31
     */
    public int getDayOfMonth() {
        return persianDate.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * Persian months.
     */
    public enum Month {
        FARVARDIN(1, 31, "فروردین"),
        ORDIBEHESHT(2, 31, "اردیبهشت"),
        KHORDAD(3, 31, "خرداد"),
        TIR(4, 31, "تیر"),
        MORDAD(5, 31, "مرداد"),
        SHAHRIVAR(6, 31, "شهریور"),
        MEHR(7, 30, "مهر"),
        ABAN(8, 30, "آبان"),
        AZAR(9, 30, "آذر"),
        DEY(10, 30, "دی"),
        BAHMAN(11, 30, "بهمن"),
        ESFAND(12, 29, "اسفند");

        private final int number;
        private final int nDays;
        private final String persianName;

        private Month(int number, int nDays, String persianName) {
            this.number = number;
            this.nDays = nDays;
            this.persianName = persianName;
        }

        /**
         * @return number of month, from 1 to 12
         */
        int number() {
            return number;
        }

        /**
         * @return maximum days of each month, in a non-leap year
         */
        int days() {
            return nDays;
        }

        /**
         * @return persian name of month.
         */
        String getPersianName() {
            return persianName;
        }

        /**
         * Returns the equivalent instance of {@code Month}, based on the passed argument.
         * Argument should be from 1 to 12.
         *
         * @param month the number of month
         * @return instance of {@code Month} enum.
         */
        static Month of(int month) {
            for (Month pm : values()) {
                if (pm.number == month) {
                    return pm;
                }
            }
            throw new IllegalArgumentException("invalid month number:' " + month + "'");
        }
    }

    /**
     * Obtains current Persian date from the system clock in the default time zone.
     *
     * @return current Persian date from the system clock in the default time zone
     */
    public static PersianDate now() {
        return gregorianToPersian(LocalDate.now());
    }

    /**
     * Obtains an instance of {@code PersianDate} with year, month, day of month, hour,
     * minute and second. The value of month must be between {@code 1} and {@code 12}.
     * Value {@code 1} would be {@link Month#FARVARDIN} and value {@code 12} represents
     * {@link Month#ESFAND}.
     *
     * @param year       the year to represent, from 1 to MAX_YEAR
     * @param month      the value of month, from 1 to 12
     * @param dayOfMonth the dayOfMonth to represent, from 1 to 31
     * @return an instance of {@code PersianDate}
     * @throws DateTimeException if the passed parameters do not form a valid date or time.
     */
    public static PersianDate of(int year, int month, int dayOfMonth) {
        return new PersianDate(year, Month.of(month), dayOfMonth);
    }

    /**
     * Obtains an instance of {@code PersianDate} with year, month, day of month, hour,
     * minute and second.
     *
     * @param year       the year to represent, from 1 to MAX_YEAR
     * @param month      the month-of-year to represent, an instance of {@link Month}
     * @param dayOfMonth the dayOfMonth to represent, from 1 to 31
     * @return an instance of {@code PersianDate}
     * @throws DateTimeException if the passed parameters do not form a valid date or time.
     */
    public static PersianDate of(int year, Month month, int dayOfMonth) {
        return new PersianDate(year, month, dayOfMonth);
    }

    /**
     * Constructor.
     *
     * @param year       the year to represent, from 1 to MAX_YEAR
     * @param month      the month-of-year to represent, not null, from {@link Month} enum
     * @param dayOfMonth the dayOfMonth-of-month to represent, from 1 to 31
     * @throws DateTimeException if the passed parameters do not form a valid date or time.
     */
    private PersianDate(int year, Month month, int dayOfMonth) {

        if (!isBetween(year, 0, Year.MAX_VALUE)) {
            throw new DateTimeException("year is out of range: '" + year + "'");
        }

        Objects.requireNonNull(month, "month must not be null");
        if (!isBetween(month.number, 1, 12)) {
            throw new DateTimeException("month is out of range: '" + month + "'");
        }

        if (dayOfMonth > 29) {
            int maxValidDays = isLeapYear(year) && month == Month.ESFAND ? 30 : month.nDays;
            if (!isBetween(dayOfMonth, 1, maxValidDays)) {
                if (!isLeapYear(year) && month == Month.ESFAND && dayOfMonth == 30) {
                    throw new DateTimeException("Invalid date 'ESFAND 30' as '" + year + "' is not a leap year");
                } else {
                    throw new DateTimeException("Invalid date '" + month.toString() + " " + dayOfMonth + "'");
                }
            }
        }

        // Create Persian date
        persianDate = Calendar.getInstance(new ULocale("fa_IR@calendar=persian"));
        persianDate.clear();
        persianDate.set(year, month.number - 1, dayOfMonth);

        // Convert Persian to Gregorian
        java.util.Calendar gregorianDate = GregorianCalendar.getInstance();
        gregorianDate.setTime(persianDate.getTime());

        int gregYear = gregorianDate.get(Calendar.YEAR);
        int gregMonth = gregorianDate.get(Calendar.MONTH) + 1;
        int gregDayOfMonth = gregorianDate.get(Calendar.DAY_OF_MONTH);
        this.gregDate = LocalDate.of(gregYear, gregMonth, gregDayOfMonth);
    }

    /**
     * Returns true if and only if {@code val} is greater than or equal to {@code lowerLimit}
     * and is less than or equal to {@code upperLimit}.
     *
     * @param val        the value to be checked
     * @param lowerLimit lower boundary to be checked
     * @param upperLimit upper boundary to be checked
     * @return true if and only if {@code val} is between {@code lowerLimit} and
     * {@code upperLimit}
     */
    private static boolean isBetween(int val, int lowerLimit, int upperLimit) {
        return val >= lowerLimit && val <= upperLimit;
    }

    /**
     * Returns an equivalent Gregorian date and time as an instance of {@link LocalDate}.
     * Calling this method has no effect on the object that calls this.
     *
     * @return the equivalent Gregorian date as an instance of {@link LocalDate}
     */
    public LocalDate toGregorian() {
        return gregDate;
    }

    /**
     * Returns an equivalent Persian date and time as an instance of {@link PersianDate}.
     * Calling this method has no effect on the class and other instances of this class.
     *
     * @param ldt Gregorian date and time
     * @return an equivalent Persian date and time as an instance of {@link PersianDate}
     */
    public static PersianDate gregorianToPersian(LocalDate ldt) {
        // Create a Gregorian calendar with the argument ldt
        java.util.Calendar gregorianCalendar = java.util.Calendar.getInstance();
        gregorianCalendar.set(ldt.getYear(), ldt.getMonthValue() - 1, ldt.getDayOfMonth());

        // Convert Gregorian calendar to Persian calendar
        ULocale locale = new ULocale("fa_IR@calendar=persian");
        Calendar persianCalendar = Calendar.getInstance(locale);
        persianCalendar.setTime(gregorianCalendar.getTime());

        int persianYear = persianCalendar.get(Calendar.YEAR);
        int persianMonth = persianCalendar.get(Calendar.MONTH) + 1;
        int persianDayOfMonth = persianCalendar.get(Calendar.DAY_OF_MONTH);
        return PersianDate.of(persianYear, persianMonth, persianDayOfMonth);
    }

    /**
     * Returns true if {@code year} is a leap year in Persian calendar.
     *
     * @return true if {@code year} is a leap year in Persian calendar
     */
    public boolean isLeapYear() {
        return PersianDate.isLeapYear(getYear());
    }

    /**
     * Returns true if {@code year} is a leap year in Persian calendar.
     *
     * @return true if {@code year} is a leap year in Persian calendar
     * @throws IllegalArgumentException if argument is a negative value
     */
    public static boolean isLeapYear(int year) {
        if (year < 0) {
            throw new IllegalArgumentException("year: '" + year + "' must be a positive integer");
        }
        return (((25 * year) + 11) % 33) < 8;
    }

    //-----------------------------------------------------------------------
    /**
     * Compares this persian date to another persian date.
     * <p>
     * Newer persian date is considered greater.
     *
     * @param other the other persian date to compare to, not null
     * @return the comparator value, negative if less, positive if greater and zero if equals
     */
    @Override
    public int compareTo(PersianDate other) {
        Objects.requireNonNull(other, "object to compare must not be null");
        return this.gregDate.compareTo(other.gregDate);
    }

    /**
     * Checks whether this persian date has the same date with the specified persian date.
     *
     * @param other instance of persian date to compare to
     * @return true, if this persian date and specified persian dates have the same date value.
     */
    public boolean isEqual(PersianDate other){
        return compareTo(other) == 0;
    }

    /**
     * Checks whether this persian date is before the specified persian date.
     *
     * <pre>
     *     PersianDate a = PersianDate.of(1396, 3, 15);
     *     PersianDate b = PersianDate.of(1396, 6, 10);
     *     a.isBefore(b) -> true
     *     a.isBefore(a) -> false
     *     b.isBefore(a) -> false
     * </pre>
     * @param other instance of PersianDate to compare to
     * @return true, if persian date is before the specified persian date
     */
    public boolean isBefore(PersianDate other){
        return compareTo(other) < 0;
    }

    /**
     * Checks whether this persian date is after the specified persian date.
     *
     * <pre>
     *     PersianDate a = PersianDate.of(1396, 3, 15);
     *     PersianDate b = PersianDate.of(1396, 6, 10);
     *     a.isAfter(b) -> false
     *     a.isAfter(a) -> false
     *     b.isAfter(a) -> true
     * </pre>
     * @param other instance of PersianDate to compare to
     * @return true, if persian date is after the specified persian date
     */
    public boolean isAfter(PersianDate other){
        return compareTo(other) > 0;
    }

    //-----------------------------------------------------------------------
    /**
     * Checks if this date is equal to another date.
     * <p>
     * Compares this {@code LocalDate} with another ensuring that the date is the same.
     *
     * @param obj  the object to check, null returns false
     * @return true if this is equal to the other date
     */
    @Override
    public boolean equals(Object obj){
        if(this == obj){
            return true;
        }
        if(obj instanceof PersianDate){
            return compareTo((PersianDate) obj) == 0;
        }
        return false;
    }

    /**
     * A hash code for this persian date.
     *
     * @return a suitable hash code
     */
    @Override
    public int hashCode(){
        int result = 17;
        result = 31 * result + getYear();
        result = 31 * result + getMonthValue();
        result = 31 * result + getDayOfMonth();
        return result;
    }

    //-----------------------------------------------------------------------
    /**
     * Returns the string representation of this persian date. The string contains of ten
     * characters whose format is "XXXX-YY-ZZ", where XXXX is the year, YY is the
     * month-of-year and ZZ is day-of-month. (Each of the capital characters represents a
     * single decimal digit.)
     * <p>
     * If any of the three parts of this persian date is too small to fill up its field,
     * the field is padded with leading zeros.
     *
     * @return a suitable representation of this persian date
     */
    public String toString(){
        return String.format("%04d-%02d-%02d", getYear(), getMonthValue(), getDayOfMonth());
    }
}