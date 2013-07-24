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
package org.jtalks.jcommune.model.dao;

import org.jtalks.common.model.dao.Crud;
import org.jtalks.common.model.entity.Section;
import org.jtalks.jcommune.model.entity.Branch;
import org.jtalks.jcommune.model.entity.JCUser;
import org.jtalks.jcommune.model.entity.SubscriptionAwareEntity;

import java.util.Collection;
import java.util.List;

/**
 * DAO for the {@link Branch} objects.
 *
 * @author Vitaliy Kravchenko
 * @author Max Malakhov
 * @author Evgeniy Naumenko
 * @author Eugeny Batov
 * @author masyan
 * @see org.jtalks.jcommune.model.dao.hibernate.BranchHibernateDao
 */

public interface BranchDao extends Crud<Branch> {

    /**
     * Get all available for user branches sorted by section position as primary order and
     * branch position and secondary order
     *
     * @param user the user
     * @return list of {@code Branch} objects
     */
    List<Branch> getAllAvailableBranches(JCUser user);

    /**
     * Get all available for user branches in specified section
     * sorted by section position as primary order and branch position and secondary order
     *
     * @param user the user
     * @param section the section
     * @return list of {@code Branch} objects
     */
    List<Branch> getAllAvailableBranchesInSection(JCUser user, Section section);

    /**
     * Get count of posts in the branch.
     *
     * @param branch the branch
     * @return count of posts in the branch
     */
    int getCountPostsInBranch(Branch branch);

    /**
     * Get state of unread posts in the branch.
     *
     * @param branch the branch
     * @param user   the user
     * @return state of unread posts in the branch for user
     */
    boolean isUnreadPostsInBranch(Branch branch, JCUser user);

    /**
     * Get subscribers for specified branch with allowed permission to read this branch.
     *
     * @param entity the branch
     * @return subscribers with allowed permission
     */
    Collection<JCUser> getAllowedSubscribers(SubscriptionAwareEntity entity);
}