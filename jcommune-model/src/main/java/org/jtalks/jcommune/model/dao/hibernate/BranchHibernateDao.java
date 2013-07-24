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
package org.jtalks.jcommune.model.dao.hibernate;

import org.hibernate.SessionFactory;
import org.jtalks.common.model.dao.hibernate.GenericDao;
import org.jtalks.common.model.entity.Entity;
import org.jtalks.common.model.entity.Section;
import org.jtalks.jcommune.model.dao.BranchDao;
import org.jtalks.jcommune.model.entity.Branch;
import org.jtalks.jcommune.model.entity.JCUser;
import org.jtalks.jcommune.model.entity.SubscriptionAwareEntity;

import java.util.*;

/**
 * Hibernate DAO implementation for operations with a {@link Branch}.
 *
 * @author Vitaliy Kravchenko
 * @author Max Malakhov
 * @author Eugeny Batov
 * @author Anuar Nurmakanov
 * @author masyan
 */
public class BranchHibernateDao extends GenericDao<Branch>
        implements BranchDao {

    /**
     * @param sessionFactory The SessionFactory.
     */
    public BranchHibernateDao(SessionFactory sessionFactory) {
        super(sessionFactory, Branch.class);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Branch> getAllAvailableBranches(JCUser user) {
        List<Branch> branches = session()
                .getNamedQuery("getAllAvailableBranches")
                .setParameterList("groupIds", getEntityIdsStrings(new ArrayList<Entity>(user.getGroups())))
                .list();
        return branches;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Branch> getAllAvailableBranchesInSection(JCUser user, Section section) {
        List<Branch> branches = session()
                .getNamedQuery("getAllAvailableBranchesInSection")
                .setParameterList("groupIds", getEntityIdsStrings(new ArrayList<Entity>(user.getGroups())))
                .setParameter("section", section)
                .list();
        return branches;
    }

    /**
     * Return entity ids from list entities as strings
     *
     * @param entities entities
     * @return id's entities
     */
    private List<String> getEntityIdsStrings(List<Entity> entities) {
        List<String> ids = new ArrayList<String>();
        for (Entity e : entities) {
            ids.add(e.getId() + "");
        }
        return ids;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCountPostsInBranch(Branch branch) {
        Number count = (Number) session()
                .getNamedQuery("getCountPostsInBranch")
                .setParameter("branch", branch)
                .uniqueResult();
        return count.intValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUnreadPostsInBranch(Branch branch, JCUser user) {
        Number count = (Number) session()
                .getNamedQuery("getCountUnreadPostsInBranch")
                .setParameter("user", user.getId())
                .setParameter("branch", branch.getId())
                .uniqueResult();
        return count.intValue() > 0 ? true : false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<JCUser> getAllowedSubscribers(SubscriptionAwareEntity entity){
        // use set for remove duplicates
        @SuppressWarnings("unchecked")
        Set<JCUser> foundUsers = new HashSet(session()
                .getNamedQuery("getAllowedSubscribersForBranch")
                .setParameter("branch", entity)
                .list());
        return foundUsers;
    }
}