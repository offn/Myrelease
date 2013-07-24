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

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.joda.time.DateTime;
import org.jtalks.jcommune.model.PersistedObjectsFactory;
import org.jtalks.jcommune.model.dao.PostDao;
import org.jtalks.jcommune.model.dto.JCommunePageRequest;
import org.jtalks.jcommune.model.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.*;

/**
 * @author Kirill Afonin
 */
@ContextConfiguration(locations = {"classpath:/org/jtalks/jcommune/model/entity/applicationContext-dao.xml"})
@TransactionConfiguration(transactionManager = "transactionManager", defaultRollback = true)
@Transactional
public class PostHibernateDaoTest extends AbstractTransactionalTestNGSpringContextTests {

    private static final int PAGE_NUMBER_TOO_LOW = 0;
    private static final int PAGE_NUMBER_TOO_BIG = 1000;
    @Autowired
    private SessionFactory sessionFactory;
    @Autowired
    private PostDao dao;
    private Session session;

    @BeforeMethod
    public void setUp() {
        session = sessionFactory.getCurrentSession();
        PersistedObjectsFactory.setSession(session);
    }

    /*===== Common methods =====*/

    @Test
    public void testGet() {
        Post post = PersistedObjectsFactory.getDefaultPost();
        session.save(post);

        Post result = dao.get(post.getId());

        assertNotNull(result);
        assertEquals(result.getId(), post.getId());
    }

    @Test
    public void testGetInvalidId() {
        Post result = dao.get(-567890L);

        assertNull(result);
    }

    @Test
    public void testUpdate() {
        String newContent = "new content";
        Post post = PersistedObjectsFactory.getDefaultPost();
        session.save(post);
        post.setPostContent(newContent);

        dao.saveOrUpdate(post);
        session.evict(post);
        Post result = (Post) session.get(Post.class, post.getId());

        assertEquals(result.getPostContent(), newContent);
    }

    @Test(expectedExceptions = org.springframework.dao.DataIntegrityViolationException.class)
    public void testUpdateNotNullViolation() {
        Post post = PersistedObjectsFactory.getDefaultPost();
        session.save(post);
        post.setPostContent(null);
        dao.saveOrUpdate(post);
    }

    /* PostDao specific methods */

    @Test
    public void testPostOfUserWithEnabledPaging() {
        int totalSize = 50;
        int pageCount = 2;
        int pageSize = totalSize / pageCount;

        JCommunePageRequest pageRequest = JCommunePageRequest.createWithPagingEnabled(1, pageSize);
        List<Post> posts = PersistedObjectsFactory.createAndSavePostList(totalSize);
        JCUser author = posts.get(0).getUserCreated();

        Page<Post> postsPage = dao.getUserPosts(author, pageRequest);

        assertEquals(postsPage.getContent().size(), pageSize, "Incorrect count of posts in one page.");
        assertEquals(postsPage.getTotalElements(), totalSize, "Incorrect total count.");
        assertEquals(postsPage.getTotalPages(), pageCount, "Incorrect count of pages.");
    }

    @Test
    public void testPostOfUserWithEnabledPagingPageLessTooLow() {
        int totalSize = 50;
        int pageCount = 2;
        int pageSize = totalSize / pageCount;

        JCommunePageRequest pageRequest = JCommunePageRequest.createWithPagingEnabled(PAGE_NUMBER_TOO_LOW, pageSize);
        List<Post> posts = PersistedObjectsFactory.createAndSavePostList(totalSize);
        JCUser author = posts.get(0).getUserCreated();

        Page<Post> postsPage = dao.getUserPosts(author, pageRequest);

        assertEquals(postsPage.getContent().size(), pageSize, "Incorrect count of posts in one page.");
        assertEquals(postsPage.getTotalElements(), totalSize, "Incorrect total count.");
        assertEquals(postsPage.getTotalPages(), pageCount, "Incorrect count of pages.");
        assertEquals(postsPage.getNumber(), 1, "Incorrect page number");
    }

    @Test
    public void testPostOfUserWithEnabledPagingPageTooBig() {
        int totalSize = 50;
        int pageCount = 2;
        int pageSize = totalSize / pageCount;

        JCommunePageRequest pageRequest = JCommunePageRequest.createWithPagingEnabled(PAGE_NUMBER_TOO_BIG, pageSize);
        List<Post> posts = PersistedObjectsFactory.createAndSavePostList(totalSize);
        JCUser author = posts.get(0).getUserCreated();

        Page<Post> postsPage = dao.getUserPosts(author, pageRequest);

        assertEquals(postsPage.getContent().size(), pageSize, "Incorrect count of posts in one page.");
        assertEquals(postsPage.getTotalElements(), totalSize, "Incorrect total count.");
        assertEquals(postsPage.getTotalPages(), pageCount, "Incorrect count of pages.");
        assertEquals(postsPage.getNumber(), pageCount, "Incorrect page number");
    }

    @Test
    public void testPostsOfUserWithDisabledPaging() {
        int size = 50;
        JCommunePageRequest pageRequest = JCommunePageRequest.createWithPagingDisabled(1, size / 2);
        List<Post> posts = PersistedObjectsFactory.createAndSavePostList(size);
        JCUser author = posts.get(0).getUserCreated();

        Page<Post> postsPage = dao.getUserPosts(author, pageRequest);

        assertEquals(postsPage.getContent().size(), size,
                "Paging is disabled, so it should retrieve all posts in the topic.");
        assertEquals(postsPage.getTotalElements(), size, "Incorrect total count.");
    }

    @Test
    public void testNullPostOfUser() {
        JCommunePageRequest pageRequest = JCommunePageRequest.createWithPagingEnabled(1, 50);
        JCUser user = ObjectsFactory.getDefaultUser();
        session.save(user);

        Page<Post> postsPage = dao.getUserPosts(user, pageRequest);

        assertFalse(postsPage.hasContent());
    }

    @Test
    public void testGetPostsWithEnabledPaging() {
        int totalSize = 50;
        int pageCount = 2;
        int pageSize = totalSize / pageCount;
        JCommunePageRequest pageRequest = JCommunePageRequest.createWithPagingEnabled(1, pageSize);
        List<Post> posts = PersistedObjectsFactory.createAndSavePostList(totalSize);
        Topic topic = posts.get(0).getTopic();

        Page<Post> postsPage = dao.getPosts(topic, pageRequest);

        assertEquals(postsPage.getContent().size(), pageSize, "Incorrect count of posts in one page.");
        assertEquals(postsPage.getTotalElements(), totalSize, "Incorrect total count.");
        assertEquals(postsPage.getTotalPages(), pageCount, "Incorrect count of pages.");
    }

    @Test
    public void testGetPostsWithEnabledPagingPageTooLow() {
        int totalSize = 50;
        int pageCount = 2;
        int pageSize = totalSize / pageCount;
        JCommunePageRequest pageRequest = JCommunePageRequest.createWithPagingEnabled(PAGE_NUMBER_TOO_LOW, pageSize);
        List<Post> posts = PersistedObjectsFactory.createAndSavePostList(totalSize);
        Topic topic = posts.get(0).getTopic();

        Page<Post> postsPage = dao.getPosts(topic, pageRequest);

        assertEquals(postsPage.getContent().size(), pageSize, "Incorrect count of posts in one page.");
        assertEquals(postsPage.getTotalElements(), totalSize, "Incorrect total count.");
        assertEquals(postsPage.getTotalPages(), pageCount, "Incorrect count of pages.");
        assertEquals(postsPage.getNumber(), 1, "Incorrect number of page");
    }

    @Test
    public void testGetPostsWithEnabledPagingPageTooBig() {
        int totalSize = 50;
        int pageCount = 2;
        int pageSize = totalSize / pageCount;
        JCommunePageRequest pageRequest = JCommunePageRequest.createWithPagingEnabled(PAGE_NUMBER_TOO_BIG, pageSize);
        List<Post> posts = PersistedObjectsFactory.createAndSavePostList(totalSize);
        Topic topic = posts.get(0).getTopic();

        Page<Post> postsPage = dao.getPosts(topic, pageRequest);

        assertEquals(postsPage.getContent().size(), pageSize, "Incorrect count of posts in one page.");
        assertEquals(postsPage.getTotalElements(), totalSize, "Incorrect total count.");
        assertEquals(postsPage.getTotalPages(), pageCount, "Incorrect count of pages.");
        assertEquals(postsPage.getNumber(), pageCount, "Incorrect number of page");
    }

    @Test
    public void testGetPostsWithDisabledPaging() {
        int size = 50;
        JCommunePageRequest pageRequest = JCommunePageRequest.createWithPagingDisabled(1, size / 2);
        List<Post> posts = PersistedObjectsFactory.createAndSavePostList(size);
        Topic topic = posts.get(0).getTopic();

        Page<Post> postsPage = dao.getPosts(topic, pageRequest);

        assertEquals(postsPage.getContent().size(), size,
                "Paging is disabled, so it should retrieve all posts in the topic.");
        assertEquals(postsPage.getTotalElements(), size, "Incorrect total count.");
    }

    @Test
    public void testGetLastPostForBranch() {
        int size = 2;
        List<Post> posts = PersistedObjectsFactory.createAndSavePostList(size);
        Topic postsTopic = posts.get(0).getTopic();
        Branch postsBranch = postsTopic.getBranch();
        Post expectedLastPost = posts.get(0);
        ReflectionTestUtils.setField(
                expectedLastPost,
                "creationDate",
                new DateTime(2100, 12, 25, 0, 0, 0, 0));
        session.save(expectedLastPost);

        Post actualLastPost = dao.getLastPostFor(postsBranch);

        assertNotNull(actualLastPost, "Last post in the branch is not found.");
        assertEquals(actualLastPost.getId(), expectedLastPost.getId(),
                "The last post in the branch is the wrong.");
    }

    @Test
    public void testGetLastPostInEmptyBranch() {
        Topic topic = PersistedObjectsFactory.getDefaultTopic();
        Branch branch = topic.getBranch();
        branch.deleteTopic(topic);

        session.save(branch);

        assertNull(dao.getLastPostFor(branch),
                "The branch is empty, so last post mustn't be found");
    }
}
