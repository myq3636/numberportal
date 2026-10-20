package com.liugh.util;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class PhoneUtils {
    private static Logger logger = LoggerFactory.getLogger(PhoneUtils.class);

    private static PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    public static final String PLUS_SIGN = "+";


    public static String getCountryCodeByPhoneStr(final String phone) {
        final int code = getCountryCodeByPhone(phone, -1);
        return code != -1 ? String.valueOf(code) : null;
    }


    public static int getCountryCodeByPhone(final String phone, int defaultCountryCode) {
        int countryCode = getCountryCode0(normalizePhoneIfPossible(phone));
        return countryCode != -1 ? countryCode : defaultCountryCode;
    }


    /**
     * 获取区号。例如好几个国家使用同一个 contry code，需要区号来区分手机号属于哪个国家
     *
     * @param phone
     * @return
     */
    public static String getRegionCodeByPhone(final String phone) {
        final Phonenumber.PhoneNumber phoneNumber = parse(phone);
        return phoneNumber != null ? phoneNumberUtil.getRegionCodeForNumber(phoneNumber) : "null";
        //return phoneNumber != null ? phoneNumberUtil.getRegionCodeForNumber(phoneNumber) : null;
        //        System.out.println(PhoneNumberToCarrierMapper.getInstance().getNameForNumber(phoneNumber,Locale.ENGLISH));
        //        System.out.println(PhoneNumberOfflineGeocoder.getInstance().getDescriptionForNumber(phoneNumber,Locale.ENGLISH));
    }


    public static boolean isValidRegionCode(String regionCode) {
        return regionCode != null && phoneNumberUtil.getSupportedRegions().contains(regionCode);
    }


    public static boolean matchPhoneSegments(String phone, Set<String> phoneSegs) {
        if (phoneSegs == null || phoneSegs.isEmpty()) {
            return false;
        }

        String tmp = getPhoneWithoutCountryCode(phone);
        for (String seg : phoneSegs) {
            if (tmp.startsWith(seg)) {
                return true;
            }
        }

        return false;

    }


    public static String getPhoneWithoutCountryCode(final String phone) {
        final Phonenumber.PhoneNumber phoneNumber = parse(phone);
        return phoneNumber != null ? String.valueOf(phoneNumber.getNationalNumber())
                : normalizePhoneIfPossible(phone);
    }


    /**
     * 尽量格式化成 + 开始的国际标准格式，不行就原样返回
     *
     * @param phone
     * @return
     */
    public static String normalizePhoneIfPossible(String phone) {
        phone = formatPhoneWith00IfNoPrefix(phone);
        if (StringUtils.isBlank(phone)) {
            return phone;
        }

        phone = phone.replaceAll("-|\\s+|\\(|\\)", "");

        if (phone.startsWith("0")) {
            return PLUS_SIGN + StringUtils.stripStart(phone, "0");
        }

        if (phone.startsWith(PLUS_SIGN)) {
            return phone;
        }

        //如果不为0或+开始，则原样返回

        return phone;
    }

    /**
     * 手机号格式化成带"+"号的形式，例如： +8613812345678
     */
    public static String formatPhoneWithPlusSign(String phone) {
        if (StringUtils.isBlank(phone)) {
            return phone;
        }
        phone = phone.replaceAll("-|\\s+|\\(|\\)", "");
        if (phone.startsWith("00")) {
            return PLUS_SIGN + StringUtils.stripStart(phone, "00");
        } else if (phone.startsWith(PLUS_SIGN)) {
            return phone;
        } else {
            return PLUS_SIGN + phone;
        }
    }

    private static int getCountryCode0(final String phone) {
        final Phonenumber.PhoneNumber phoneNumber = parse(phone);
        return phoneNumber != null ? phoneNumber.getCountryCode() : -1;
    }


    public static Phonenumber.PhoneNumber parse(String phone) {
        phone = normalizePhoneIfPossible(phone);
        try {
            return phoneNumberUtil.parse(phone, "");
        } catch (NumberParseException e) {
            logger.warn("invalid phone format:{},error:{}", phone, e.getMessage());
        }
        return null;
    }


    public static boolean isFullPhoneNumber(String phone) {
        try {
            phoneNumberUtil.parse(phone, "");
            return true;
        } catch (NumberParseException e) {
        }
        return false;
    }

    /**
     * 手机号格式化成带"00"号的形式，例如： 008613812345678 date:2019-08-06
     */
    public static String formatPhoneWith00IfNoPrefix(String phone) {
        if (StringUtils.isBlank(phone)) {
            return phone;
        }
        String filterPhone = phone.replaceAll("-|\\s+|\\(|\\)", "");
        if (filterPhone.startsWith("00") || filterPhone.startsWith(PLUS_SIGN)) {
            return phone;
        } else {
            filterPhone = "00" + filterPhone;
            logger.info("formatPhoneWith00IfNoPrefix, originalPhone:{},filterPhone:{}", phone, filterPhone);
            return filterPhone;
        }
    }

    public static void main(String[] args) {
    	String mobile = "628201109958";
    	//String countryCode = PhoneUtils.getRegionCodeByPhone(mobile);
    	String countryCode = PhoneUtils.getCountryCodeByPhoneStr(mobile);
    	System.out.println(countryCode);
	}
}
