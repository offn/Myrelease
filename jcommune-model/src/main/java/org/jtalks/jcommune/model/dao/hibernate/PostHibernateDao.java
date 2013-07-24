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

import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.jtalks.common.model.dao.hibernate.GenericDao;
import org.jtalks.jcommune.model.dao.PostDao;
import org.jtalks.jcommune.model.dto.JCommunePageRequest;
import org.jtalks.jcommune.model.entity.Branch;
import org.jtalks.jcommune.model.entity.JCUser;
import org.jtalks.jcommune.model.entity.Post;
import org.jtalks.jcommune.model.entity.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

/**
 * The implementation of PostDao based on Hibernate.
 * The class is responsible for loading {@link Post} objects from database,
 * save, update and delete them.
 *
 * @author Pavel Vervenko
 * @author Kirill Afonin
 * @author Anuar Nurmakanov
 */
public class PostHibernateDao extends GenericDao<Post> implements PostDao {
    private static final String TOPIC_PARAMETER_NAME = "topic";

    /**
     * @param sessionFactory The SessionFactory.
     */
    public PostHibernateDao(SessionFactory sessionFactory) {
        super(sessionFactory, Post.class);
    }

    /**
     * {@inheritDoc}
     */
    public Page<Post> getUserPosts(JCUser author, JCommunePageRequest pageRequest) {
        Number totalCount = (Number) session()
                .getNamedQuery("getCountPostsOfUser")
                .setParameter("userCreated", author)
                .uniqueResult();
        Query query = session()
                .getNamedQuery("getPostsOfUser")
                .setParameter("userCreated", author);
        if (pageRequest.isPagingEnabled()) {
            pageRequest.adjustPageNumber(totalCount.intValue());
            query.setFirstResult(pageRequest.getOffset());
            query.setMaxResults(pageRequest.getPageSize());
        }
        @SuppressWarnings("unchecked")
        List<Post> posts = (List<Post>) query.list();
        return new PageImpl<Post>(posts, pageRequest, totalCount.intValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<Post> getPosts(Topic topic, JCommunePageRequest pageRequest) {
        Number totalCount = (Number) session()
                .getNamedQuery("getCountPostsInTopic")
                .setParameter(TOPIC_PARAMETER_NAME, topic)
                .uniqueResult();
        Query query = session()
                .getNamedQuery("getPostsInTopic")
                .setParameter(TOPIC_PARAMETER_NAME, topic);
        if (pageRequest.isPagingEnabled()) {
            pageRequest.adjustPageNumber(totalCount.intValue());
            query.setFirstResult(pageRequest.getOffset());
            query.setMaxResults(pageRequest.getPageSize());
        }
        @SuppressWarnings("unchecked")
        List<Post> posts = (List<Post>) query.list();
        return new PageImpl<Post>(posts, pageRequest, totalCount.intValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Post getLastPostFor(Branch branch) {
        Post result = (Post) session()
                .getNamedQuery("getLastPostForBranch")
                .setParameter("branch", branch)
                .setMaxResults(1)
                .uniqueResult();
        return result;
    }
}
