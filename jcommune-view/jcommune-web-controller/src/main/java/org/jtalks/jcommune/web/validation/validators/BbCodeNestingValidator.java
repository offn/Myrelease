/**
 * Copyright (C) 2011  JTalks.org Team
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jtalks.jcommune.web.validation.validators;

import org.jtalks.jcommune.service.UserService;
import org.jtalks.jcommune.web.validation.annotations.BbCodeNesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checked nesting bb code level (maxNestingValue). This is required because our BB-code processor uses recursion for
 * parsing and if we have a deep nesting, we'll run into StackOverflow error. Thus before posting something, we check
 * whether the nesting of BB-codes is not too deep.
 */
public class BbCodeNestingValidator implements ConstraintValidator<BbCodeNesting, String> {
    private static final String TAG_LIST_ITEM = "[*]";

    private static final Logger LOGGER = LoggerFactory.getLogger(BbCodeNestingValidator.class);
    private UserService userService;
    private int maxNestingValue;

    /**
     * Constructor
     *
     * @param userService user service
     */
    @Autowired
    public BbCodeNestingValidator(UserService userService) {
        this.userService = userService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(BbCodeNesting constraintAnnotation) {
        maxNestingValue = constraintAnnotation.maxNestingValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (!checkForDifferent(value) ||
                !checkForQuote(value)) {
            return false;
        }
        return true;
    }

    /**
     * Checked nesting bb code level (maxNestingValue)
     *
     * @param text  text with bb code
     * @param regex regular expression for check
     * @return allowed or not allowed
     */
    private boolean checkNestingLevel(String text, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            String tag = matcher.group();
            if (tag.isEmpty() || tag.equals(TAG_LIST_ITEM)) {
                continue;
            }
            if (tag.length() > 1 && tag.charAt(1) == '/') {
                count--;
            } else {
                count++;
            }
            if (Math.abs(count) > maxNestingValue) {
                LOGGER.warn("Possible attack: Too deep bb-code nesting. "
                        + "User UUID: {}", userService.getCurrentUser().getUuid());
                return false;
            }
        }
        return true;
    }

    /**
     * Check nesting level for different bb-codes
     *
     * @param value text value
     * @return allowed or not allowed
     */
    private boolean checkForDifferent(String value) {
        final String regexForSimples = "\\[[^\\[\\]]*\\]|\\[/[^\\[\\]]*\\]";
        return checkNestingLevel(value, regexForSimples);
    }

    /**
     * Check nesting level for quote bb-code, This bb-code can be in their element with another bb codes.
     *
     * @param value text value
     * @return allowed or not allowed
     */
    private boolean checkForQuote(String value) {
        final String regexForQuote = "\\[quote=\".*?\"\\]";
        String cleanValue = value.replace(" ","");
        return checkNestingLevel(cleanValue, regexForQuote);
    }
}
