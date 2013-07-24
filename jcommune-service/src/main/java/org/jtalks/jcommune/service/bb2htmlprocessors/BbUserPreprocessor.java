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

package org.jtalks.jcommune.service.bb2htmlprocessors;

import org.jtalks.jcommune.service.UserService;
import ru.perm.kefir.bbcode.TextProcessorAdapter;

/**
 * Process for [user][/user] code. It adds link to user mentioned in tag before starting
 * converting to HTML tags.  
 * 
 * @author Anuar_Nurmakanov
 *
 */
public class BbUserPreprocessor extends TextProcessorAdapter {
    private final UserService userService;

    /**
     * @param userService to check users' existence
     */
    public BbUserPreprocessor(UserService userService) {
        this.userService = userService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CharSequence process(CharSequence source) {
        String notProcessedSource = source.toString();
        return userService.processUserBbCodesInPost(notProcessedSource);
    }

}
