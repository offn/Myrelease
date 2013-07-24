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
package org.jtalks.jcommune.service.transactional;

import org.hibernate.TransientObjectException;
import org.joda.time.DateTime;
import org.jtalks.jcommune.model.dao.LastReadPostDao;
import org.jtalks.jcommune.model.dao.UserDao;
import org.jtalks.jcommune.model.entity.*;
import org.jtalks.jcommune.service.UserService;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.testng.Assert.*;

/**
 * @author Evgeniy Naumenko
 * @author Anuar_Nurmakanov
 */
public class TransactionalLastReadPostServiceTest {
    private JCUser user;

    @Mock
    private LastReadPostDao lastReadPostDao;
    @Mock
    private UserService userService;
    @Mock
    private UserDao userDao;
    //
    private TransactionalLastReadPostService lastReadPostService;


    @BeforeMethod
    public void setUp() throws Exception {
        initMocks(this);
        user = new JCUser("username", "email@mail.com", "password");
        lastReadPostService = new TransactionalLastReadPostService(
                userService,
                lastReadPostDao,
                userDao);
    }

    @Test
    public void userShouldNotSeeUpdatesWhenForumMarkedAsAllReadAndTopicsDoNotHaveModificationsAfter() {
        List<Topic> topicList = ObjectsFactory.topics(user, 1);
        when(lastReadPostDao.getLastReadPosts(user, Collections.<Topic>emptyList()))
                .thenReturn(Collections.<LastReadPost>emptyList());
        DateTime forumMarkedAsReadDate = new DateTime().plusYears(1);
        user.setAllForumMarkedAsReadTime(forumMarkedAsReadDate);
        when(userService.getCurrentUser()).thenReturn(user);

        List<Topic> result = lastReadPostService.fillLastReadPostForTopics(topicList);

        assertEquals(1, result.size());
        assertFalse(result.get(0).isHasUpdates());
    }

    @Test
    public void userShouldSeeUpdatesWhenForumMarkedAsAllReadAndTopicsHaveModificationsAfter() {
        DateTime forumMarkedAsReadDate = new DateTime().minusYears(1);
        user.setAllForumMarkedAsReadTime(forumMarkedAsReadDate);
        when(userService.getCurrentUser()).thenReturn(user);
        List<Topic> topics = ObjectsFactory.topics(user, 1);
        when(lastReadPostDao.getLastReadPosts(user, Collections.<Topic>emptyList()))
                .thenReturn(Collections.<LastReadPost>emptyList());

        List<Topic> result = lastReadPostService.fillLastReadPostForTopics(topics);

        assertEquals(1, result.size());
        assertTrue(result.get(0).isHasUpdates());
    }

    @Test
    public void authenticatedUserShouldSeeReadTopicAsTopicWithoutUpdates() {
        List<Topic> topicList = ObjectsFactory.topics(user, 1);
        LastReadPost post = new LastReadPost(user, topicList.get(0), 0);
        when(userService.getCurrentUser()).thenReturn(user);
        when(lastReadPostDao.getLastReadPosts(user, topicList)).thenReturn(Collections.singletonList(post));

        List<Topic> result = lastReadPostService.fillLastReadPostForTopics(topicList);
        assertEquals(1, result.size());
        assertFalse(result.get(0).isHasUpdates());
    }

    @Test
    public void anonymousUserShouldSeeAllTopicsAsNotRead() {
        when(userService.getCurrentUser()).thenReturn(new AnonymousUser());

        lastReadPostService.fillLastReadPostForTopics(new ArrayList<Topic>());
        verify(lastReadPostDao, never()).getLastReadPosts(Matchers.<JCUser>any(), Matchers.<List<Topic>>any());
    }

    @Test
    public void authenticatedUserShouldSeeNotReadTopicAsTopicWithUpdates() {
        List<Topic> topicList = ObjectsFactory.topics(user, 1);
        when(userService.getCurrentUser()).thenReturn(user);
        when(lastReadPostDao.getLastReadPosts(user, topicList)).thenReturn(Collections.<LastReadPost>emptyList());

        List<Topic> result = lastReadPostService.fillLastReadPostForTopics(topicList);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isHasUpdates());
    }

    @Test
    public void anonymousUserShouldNotMarkTopicPageAsRead() {
        when(userService.getCurrentUser()).thenReturn(new AnonymousUser());

        Topic topic = this.createTestTopic();

        lastReadPostService.markTopicPageAsRead(topic, 1, true);
        verifyZeroInteractions(lastReadPostDao);
    }

    @Test
    public void updateLastReadPostToAuthUserWhenAllForumMarkedBefore() {
        Topic topic = this.createTestTopic();
        user.setAllForumMarkedAsReadTime(topic.getModificationDate().minusSeconds(1));
        LastReadPost post = new LastReadPost(user, topic, 0);
        when(userService.getCurrentUser()).thenReturn(user);
        when(lastReadPostDao.getLastReadPost(user, topic)).thenReturn(post);

        lastReadPostService.markTopicPageAsRead(topic, 1, false);

        verify(lastReadPostDao).saveOrUpdate(post);
    }

    @Test
    public void updateLastReadPostToAuthUserWhenAllForumMarkedNull() {
        Topic topic = this.createTestTopic();
        user.setAllForumMarkedAsReadTime(null);
        LastReadPost post = new LastReadPost(user, topic, 0);
        when(userService.getCurrentUser()).thenReturn(user);
        when(lastReadPostDao.getLastReadPost(user, topic)).thenReturn(post);

        lastReadPostService.markTopicPageAsRead(topic, 1, false);

        verify(lastReadPostDao).saveOrUpdate(post);
    }

    @Test
    public void notUpdateLastReadPostToAuthUserWhenAllForumAfter() {
        Topic topic = this.createTestTopic();
        user.setAllForumMarkedAsReadTime(topic.getModificationDate().plusSeconds(1));
        LastReadPost post = new LastReadPost(user, topic, 0);
        when(userService.getCurrentUser()).thenReturn(user);
        when(lastReadPostDao.getLastReadPost(user, topic)).thenReturn(post);

        lastReadPostService.markTopicPageAsRead(topic, 1, false);

        verify(lastReadPostDao, never()).saveOrUpdate(post);
    }

    @Test
    public void testMarkTopicPageAsReadPagingEnabled() {
        Topic topic = this.createTestTopic();
        user.setPageSize(3);
        when(userService.getCurrentUser()).thenReturn(user);

        lastReadPostService.markTopicPageAsRead(topic, 2, true);

        verify(lastReadPostDao).saveOrUpdate(argThat(
                new LastReadPostMatcher(topic, 5)));
    }

    @Test
    public void markTopicPageAsReadShouldReupdateLastReadPostInRepository() {
        Topic topic = this.createTestTopic();
        LastReadPost post = new LastReadPost(user, topic, 0);
        when(userService.getCurrentUser()).thenReturn(user);
        when(lastReadPostDao.getLastReadPost(user, topic)).thenReturn(post);

        lastReadPostService.markTopicPageAsRead(topic, 1, false);

        verify(lastReadPostDao).saveOrUpdate(argThat(
                new LastReadPostMatcher(topic, topic.getPostCount() - 1)));
    }

    @Test
    public void testMarkTopicPageAsReadUpdateExistingDbRecordWithWrongPostIndex() {
        Topic topic = this.createTestTopic();
        LastReadPost post = new LastReadPost(user, topic, 1000);
        when(userService.getCurrentUser()).thenReturn(user);
        when(lastReadPostDao.getLastReadPost(user, topic)).thenReturn(post);

        lastReadPostService.markTopicPageAsRead(topic, 1, false);

        verify(lastReadPostDao).saveOrUpdate(argThat(
                new LastReadPostMatcher(topic, topic.getPostCount() - 1)));
    }

    @Test
    public void anonymousUserShouldNotMarkAllTopicsInBranchAsRead() {
        when(userService.getCurrentUser()).thenReturn(new AnonymousUser());
        Branch branch = new Branch("branch name", "branch description");

        lastReadPostService.markAllTopicsAsRead(branch);
        verifyZeroInteractions(lastReadPostDao);
    }

    @Test
    public void authenticatedUserShouldHaveAbilityToMarkAllTopicsInBranchAsRead() throws Exception {
        Branch branch = new Branch("branch name", "branch description");
        when(userService.getCurrentUser()).thenReturn(user);

        lastReadPostService.markAllTopicsAsRead(branch);

        verify(userService).getCurrentUser();
        verify(lastReadPostDao).markAllRead(user, branch);
    }

    @Test
    public void authenticatedUserShouldHaveAbilityToMarkTopicAsRead() {
        Topic topic = this.createTestTopic();
        when(userService.getCurrentUser()).thenReturn(user);

        lastReadPostService.markTopicAsRead(topic);
        verify(lastReadPostDao).saveOrUpdate(argThat(new LastReadPostMatcher(topic, topic.getPostCount() - 1)));
    }

    @Test
    public void anonymousUserShouldNotHaveAbilityToMarkTopicAsRead() {
        when(userService.getCurrentUser()).thenReturn(new AnonymousUser());

        Topic topic = this.createTestTopic();

        lastReadPostService.markTopicAsRead(topic);
        verifyZeroInteractions(lastReadPostDao);
    }

    @Test
    public void markTopicAsReadShouldReupdateLastReadPostInRepository() {
        Topic topic = this.createTestTopic();
        LastReadPost post = new LastReadPost(user, topic, 0);
        when(userService.getCurrentUser()).thenReturn(user);
        when(lastReadPostDao.getLastReadPost(user, topic)).thenReturn(post);

        lastReadPostService.markTopicAsRead(topic);

        verify(lastReadPostDao).saveOrUpdate(argThat(
                new LastReadPostMatcher(topic, topic.getPostCount() - 1)));
    }

    @Test
    public void testMarkTopicReadUpdateExistingDbRecordWithWrongPostIndex() {
        Topic topic = this.createTestTopic();
        LastReadPost post = new LastReadPost(user, topic, 1000);
        when(userService.getCurrentUser()).thenReturn(user);
        when(lastReadPostDao.getLastReadPost(user, topic)).thenReturn(post);

        lastReadPostService.markTopicAsRead(topic);

        verify(lastReadPostDao).saveOrUpdate(argThat(
                new LastReadPostMatcher(topic, topic.getPostCount() - 1)));
    }

    @Test
    public void getLastReadPostInTopicShouldReturnLastReadByUser() {
        Topic topic = this.createTestTopic();
        LastReadPost post = new LastReadPost(user, topic, 1);
        when(userService.getCurrentUser()).thenReturn(user);
        when(lastReadPostDao.getLastReadPosts(user, Arrays.asList(topic)))
                .thenReturn(Collections.singletonList(post));

        int actual = lastReadPostService.getLastReadPostForTopic(topic);

        assertEquals(actual, post.getPostIndex());
    }

    @Test
    public void getLastReadPostInTopicShouldReturnNullWhenUserDidNotReadTopic() {
        Topic topic = createTestTopic();
        when(userService.getCurrentUser()).thenReturn(user);
        when(lastReadPostDao.getLastReadPosts(user, Arrays.asList(topic)))
                .thenReturn(Collections.<LastReadPost>emptyList());

        assertNull(lastReadPostService.getLastReadPostForTopic(topic));
    }

    @Test
    public void getLastReadPostInTopicShouldReturnNullForAnonymousUser() {
        Topic topic = this.createTestTopic();
        JCUser anonymous = new AnonymousUser();
        when(userService.getCurrentUser()).thenReturn(anonymous);
        when(lastReadPostDao.getLastReadPost(anonymous, topic))
                .thenThrow(new TransientObjectException("Object reference to unsaved object"));

        assertNull(lastReadPostService.getLastReadPostForTopic(topic));
    }

    @Test
    public void updateAfterPostDeletingShouldUpdateLastReadPostWhenIndexChanged() {
        Topic topic = createTestTopic();
        int postIndex = 1;
        LastReadPost lastReadPost = new LastReadPost(user, topic, postIndex);
        List<LastReadPost> lastReadPosts = Arrays.asList(lastReadPost);

        when(lastReadPostDao.getLastReadPostsInTopic(topic)).thenReturn(lastReadPosts);

        lastReadPostService.updateLastReadPostsWhenPostDeleted(topic.getFirstPost());

        verify(lastReadPostDao).saveOrUpdate(lastReadPost);
        assertEquals(lastReadPost.getPostIndex(), postIndex - 1, "The index should be reduced.");
    }

    @Test
    public void updateAfterPostDeletingShouldNotUpdateLastReadPostWhenIndexNotChanged() {
        Topic topic = createTestTopic();
        int postIndex = 0;
        LastReadPost lastReadPost = new LastReadPost(user, topic, postIndex);
        List<LastReadPost> lastReadPosts = Arrays.asList(lastReadPost);
        when(lastReadPostDao.getLastReadPostsInTopic(topic)).thenReturn(lastReadPosts);

        lastReadPostService.updateLastReadPostsWhenPostDeleted(topic.getLastPost());

        verify(lastReadPostDao, never()).saveOrUpdate(lastReadPost);
        assertEquals(lastReadPost.getPostIndex(), postIndex, "The index shouldn't be reduced.");
    }

    @Test
    public void markAllForumAsReadShouldRememberMarkDateAndClearLastReadPostsForUser() {
        JCUser user = new JCUser("user", "use@gmail.com", "gangam-style-password");
        when(userService.getCurrentUser()).thenReturn(user);

        lastReadPostService.markAllForumAsReadForCurrentUser();

        assertNotNull(user.getAllForumMarkedAsReadTime(), "Mark date should be remembered for user.");
        verify(userDao).saveOrUpdate(user);
        verify(lastReadPostDao).deleteLastReadPostsFor(user);

    }

    private Topic createTestTopic() {
        Topic topic = new Topic(user, "title");
        for (int i = 0; i < 10; i++) {
            topic.addPost(new Post(user, "content"));
        }
        return topic;
    }

    private class LastReadPostMatcher extends ArgumentMatcher<LastReadPost> {

        private Topic topic;
        private int index;

        private LastReadPostMatcher(Topic topic, int index) {
            this.topic = topic;
            this.index = index;
        }

        @Override
        public boolean matches(Object argument) {
            LastReadPost post = (LastReadPost) argument;
            boolean result = post.getTopic().equals(topic);
            result &= post.getUser().equals(user);
            result &= (post.getPostIndex() == index);
            return result;
        }
    }
}
