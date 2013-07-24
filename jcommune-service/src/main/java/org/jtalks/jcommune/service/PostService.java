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
package org.jtalks.jcommune.service;


import org.jtalks.jcommune.model.entity.Branch;
import org.jtalks.jcommune.model.entity.JCUser;
import org.jtalks.jcommune.model.entity.Post;
import org.jtalks.jcommune.model.entity.Topic;
import org.jtalks.jcommune.service.exceptions.NotFoundException;
import org.springframework.data.domain.Page;

/**
 * This interface should have methods which give us more abilities in manipulating Post persistent entity.
 *
 * @author Osadchuck Eugeny
 * @author Kirill Afonin
 * @author Anuar Nurmakanov
 */
public interface PostService extends EntityService<Post> {

    /**
     * Update current post with given content, add the modification date.
     *
     * @param post      post to be updated
     * @param postContent content of post
     * @throws org.jtalks.jcommune.service.exceptions.NotFoundException
     *          when post not found
     */
    void updatePost(Post post, String postContent) throws NotFoundException;

    /**
     * Delete post
     *
     * @param post post to be deleted
     */
    void deletePost(Post post);

    /**
     * Get user's posts.
     *
     * @param userCreated user created post
     * @param page page number, for which we will find posts
     * @param pagingEnabled if true, then it returns posts for one page, otherwise it
     *        return all posts, that were created by user
     * @return object that contains posts for one page(note, that one page may contain
     *         all posts, that were created by user) and information for pagination
     */
    Page<Post> getPostsOfUser(JCUser userCreated, int page, boolean pagingEnabled);

    /**
     * Calculates page number for post based on the current user
     * paging settings and total post amount in the topic
     *
     * @param post post to find a page for
     * @return number of the page where the post will actually be
     */
    int calculatePageForPost(Post post);
    
    /**
     * Get all posts in the topic of forum.
     * 
     * @param topic for this topic we will find posts
     * @param page page number, for which we will find posts
     * @param pagingEnabled if true, then it returns posts for one page, otherwise it
     *        return all posts in the topic
     * @return object that contains posts for one page(note, that one page may contain
     *         all posts) and information for pagination
     */
    Page<Post> getPosts(Topic topic, int page, boolean pagingEnabled);

    /**
     * Get the last post, that was posted in a topic of branch.
     * 
     * @param branch for this branch it gets the last post
     * @return the last post that was posted in branch
     */
    Post getLastPostFor(Branch branch);
}
