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
package org.jtalks.jcommune.web.dto;

/**
 * Enumerates all possible location on the forum
 */
public enum BreadcrumbLocation {

    FORUM("sections"),
    BRANCH("branches"),
    SECTION("sections"),
    TOPIC("topics");

    //Displayed value for the HTML link
    private String name;

    /**
     * Set the Breadcrumb URL location
     *
     * @param name Breadcrumb URL location
     */
    private BreadcrumbLocation(String name) {
        this.name = name;
    }

    /**
     * Return the Breadcrumb URL location
     *
     * @return name Breadcrumb URL location
     */
    public String getName() {
        return name;
    }
}
