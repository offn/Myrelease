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

package org.jtalks.jcommune.model.entity;

import org.jtalks.common.model.entity.Component;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Class keeping administrative information about the component
 *
 * @author Andrei Alikov
 */
public class ComponentInformation {
    private static final int PARAM_MIN_SIZE = 1;

    private Long id;

    @NotNull(message = "{validation.not_null}")
    @Size(min = PARAM_MIN_SIZE, max = Component.COMPONENT_NAME_MAX_LENGTH, message = "{validation.param.length}")
    private String name;

    @NotNull(message = "{validation.not_null}")
    @Size(min = PARAM_MIN_SIZE, max = Component.COMPONENT_DESCRIPTION_MAX_LENGTH, message = "{validation.param.length}")
    private String description;

    @NotNull(message = "{validation.not_null}")
    @Size(min = 0, max = Component.COMPONENT_DESCRIPTION_MAX_LENGTH, message = "{validation.param.length}")
    private String logoTooltip;

    private String logo;

    private String icon;


    /**
     * Gets the string with encoded logo picture
     *
     * @return the string with encoded logo picture
     */
    public String getLogo() {
        return logo;
    }

    /**
     * sets the string with encoded logo picture
     *
     * @param logo string with new encoded logo picture
     */
    public void setLogo(String logo) {
        this.logo = logo;
    }

    /**
     * Gets the name of the component
     *
     * @return name of the component
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the component
     *
     * @param name new name of the component
     */
    public void setName(String name) {
        this.name = name.trim();
    }

    /**
     * Gets the description of the component
     *
     * @return description of the component
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the component
     *
     * @param description new description of the component
     */
    public void setDescription(String description) {
        this.description = description.trim();
    }

    /**
     * Gets the tooltip for the component logo
     *
     * @return tooltip for the component logo
     */
    public String getLogoTooltip() {
        return logoTooltip;
    }

    /**
     * Sets the tooltip for the component logo
     *
     * @param logoTooltip new tooltip for the component logo
     */
    public void setLogoTooltip(String logoTooltip) {
        this.logoTooltip = logoTooltip.trim();
    }

    /**
     * Gets component id
     *
     * @return component id
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the component id
     *
     * @param id new component id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets component fav icon
     *
     * @return string with new encoded icon picture
     */
    public String getIcon() {
        return icon;
    }

    /**
     * Sets component fav icon
     *
     * @param icon string with new encoded icon picture
     */
    public void setIcon(String icon) {
        this.icon = icon;
    }
}
