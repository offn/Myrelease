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

import org.apache.solr.analysis.*;
import org.hibernate.search.annotations.*;
import org.hibernate.validator.constraints.NotBlank;
import org.joda.time.DateTime;
import org.jtalks.common.model.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents the topic of the forum.
 * Contains the list of related {@link Post}.
 * All Posts will be cascade deleted with the associated Topic.
 * The fields creationDate, topicStarter and Title are required and can't be <code>null</code>
 *
 * @author Pavel Vervenko
 * @author Vitaliy Kravchenko
 * @author Max Malakhov
 * @author Anuar Nurmakanov
 */
@AnalyzerDefs({
        /*
        * Describes the analyzer for Russian.
        */
        @AnalyzerDef(name = "russianJtalksAnalyzer",
                tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
                filters = {
                        /*
                        * All "terms" of the search text will be converted to lower case.
                        */
                        @TokenFilterDef(factory = LowerCaseFilterFactory.class),
                        /*
                        * Several words in language doesn't have a significant value.
                        * These filters exclude those words from the index.
                        */
                        @TokenFilterDef(factory = StopFilterFactory.class,
                                params = {
                                        @Parameter(name = "words",
                                                value = "org/jtalks/jcommune/lucene/english_stop.txt"),
                                        @Parameter(name = "ignoreCase", value = "true")
                                }),
                        @TokenFilterDef(factory = StopFilterFactory.class,
                                params = {
                                        @Parameter(name = "words",
                                                value = "org/jtalks/jcommune/lucene/russian_stop.txt"),
                                        @Parameter(name = "ignoreCase", value = "true")
                                }),
                        /*
                        * Provides the search by a root of a word.
                        * If two words have the same root, then they are equal in the terminology of search.
                        */
                        @TokenFilterDef(factory = SnowballPorterFilterFactory.class,
                                params = @Parameter(name = "language", value = "Russian"))
                }
        ),
        /*
        * Describes the analyzer for default language(English).
        */
        @AnalyzerDef(name = "defaultJtalksAnalyzer",
                tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
                filters = {
                        @TokenFilterDef(factory = StandardFilterFactory.class),
                        /*
                        * All "terms" of the search text will be converted to lower case.
                        */
                        @TokenFilterDef(factory = LowerCaseFilterFactory.class),
                        /*
                        * Several words in language don't have a significant value.
                        * These filters exclude those words from the index.
                        */
                        @TokenFilterDef(factory = StopFilterFactory.class,
                                params = {
                                        @Parameter(name = "words",
                                                value = "org/jtalks/jcommune/lucene/english_stop.txt"),
                                        @Parameter(name = "ignoreCase", value = "true")
                                }),
                        @TokenFilterDef(factory = StopFilterFactory.class,
                                params = {
                                        @Parameter(name = "words",
                                                value = "org/jtalks/jcommune/lucene/russian_stop.txt"),
                                        @Parameter(name = "ignoreCase", value = "true")
                                }),
                        /*
                        * Provides the search by a root of a word.
                        * If two words have the same root, then they are equal in the terminology of search.
                        */
                        @TokenFilterDef(factory = SnowballPorterFilterFactory.class)
                }
        )
})
@Indexed
public class Topic extends Entity implements SubscriptionAwareEntity {

    private final static Logger LOGGER = LoggerFactory.getLogger(Topic.class);
    public static final String URL_SUFFIX = "/posts/";

    private DateTime creationDate;
    private DateTime modificationDate;
    private JCUser topicStarter;
    @NotBlank
    @Size(min = Topic.MIN_NAME_SIZE, max = Topic.MAX_NAME_SIZE)
    private String title;
    private boolean sticked;
    private boolean announcement;
    private boolean closed;
    private Branch branch;
    private int views;
    @Valid
    private Poll poll;
    private CodeReview codeReview;    
    private List<Post> posts = new ArrayList<Post>();
    private Set<JCUser> subscribers = new HashSet<JCUser>();

    // transient, makes sense for current user only if set explicitly
    private transient Integer lastReadPostIndex;

    public static final int MIN_NAME_SIZE = 1;
    public static final int MAX_NAME_SIZE = 120;

    /**
     * Name of the field in the index for Russian.
     */
    public static final String TOPIC_TITLE_FIELD_RU = "topicTitleRu";
    /**
     * Name of the field in the index for default language(English).
     */
    public static final String TOPIC_TITLE_FIELD_DEF = "topicTitle";
    /**
     * Name of the prefix for collection of posts.
     */
    public static final String TOPIC_POSTS_PREFIX = "topicPosts.";


    /**
     * Creates the Topic instance.
     */
    public Topic() {
    }

    /**
     * Creates the Topic instance with required fields.
     * Creation and modification date is set to now.
     *
     * @param topicStarter user who create the topic
     * @param title        topic title
     */
    public Topic(JCUser topicStarter, String title) {
        this.topicStarter = topicStarter;
        this.title = title;
        this.creationDate = new DateTime();
        this.modificationDate = new DateTime();
    }

    /**
     * Add new {@link Post} to the topic.
     * The method sets Posts.topic field to this Topic.
     *
     * @param post post to add
     */
    public void addPost(Post post) {
        post.setTopic(this);
        updateModificationDate();
        this.posts.add(post);
    }

    /**
     * Remove the post from the topic.
     *
     * @param postToRemove post to remove
     */
    public void removePost(Post postToRemove) {
        posts.remove(postToRemove);
    }

    /**
     * Check subscribed user on topic or not.
     *
     * @param user checked user
     * @return true if user subscribed on topic
     *         false otherwise
     */
    public boolean userSubscribed(JCUser user) {
        return subscribers.contains(user);
    }

    /**
     * Get the post creation date.
     *
     * @return the creationDate
     */
    public DateTime getCreationDate() {
        return creationDate;
    }

    /**
     * Set the post creation date.
     *
     * @param creationDate the creationDate to set
     */
    protected void setCreationDate(DateTime creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * Get the user who created the post.
     *
     * @return the userCreated
     */
    public JCUser getTopicStarter() {
        return topicStarter;
    }

    /**
     * The the author of the post.
     *
     * @param userCreated the user who create the post
     */
    protected void setTopicStarter(JCUser userCreated) {
        this.topicStarter = userCreated;
    }

    /**
     * Gets the topic name.
     *
     * @return the topicName
     */
    @Fields({
            @Field(name = TOPIC_TITLE_FIELD_RU,
                    analyzer = @Analyzer(definition = "russianJtalksAnalyzer")),
            @Field(name = TOPIC_TITLE_FIELD_DEF,
                    analyzer = @Analyzer(definition = "defaultJtalksAnalyzer"))
    })
    public String getTitle() {
        return title;
    }

    /**
     * @param newTitle new title for this topic
     */
    public void setTitle(String newTitle) {
        this.title = newTitle;
    }

    /**
     * @return content of the first post of the topic
     */
    public String getBodyText() {
        Post firstPost = getFirstPost();
        return firstPost.getPostContent();
    }


    /**
     * @return the list of posts in the topic, always not null and not empty
     */
    @IndexedEmbedded(prefix = TOPIC_POSTS_PREFIX)
    public List<Post> getPosts() {
        return posts;
    }

    /**
     * @param posts the posts to set as topic contents, must not be empty or null
     */
    protected void setPosts(List<Post> posts) {
        this.posts = posts;
    }

    /**
     * @return branch that contains the topic
     */
    public Branch getBranch() {
        return branch;
    }

    /**
     * @param branch branch to be set as topics branch
     */
    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    /**
     * @return the firstPost in the topic, topics are guaranteed to have at least the first post
     */
    public Post getFirstPost() {
        return posts.get(0);
    }
    
    /**
     * Get the last post in the topic. Topics are guaranteed to have at least the first post.
     * 
     * @return last post in the topic.
     */
    public Post getLastPost() {
        return posts.get(posts.size() - 1);
    }

    /**
     * @return date and time when theme was changed last time
     */
    public DateTime getModificationDate() {
        return modificationDate;
    }

    /**
     * @param modificationDate date and time when theme was changed last time
     */
    protected void setModificationDate(DateTime modificationDate) {
        this.modificationDate = modificationDate;
    }

    /**
     * Set modification date to now.
     *
     * @return new modification date
     */
    public DateTime updateModificationDate() {
        this.modificationDate = new DateTime();
        return this.modificationDate;
    }
    
    /**
     * Calculates modification date of topic taking it as last modification
     * date among its posts.
     */
    public void recalculateModificationDate() {
        DateTime newTopicModificationDate = getFirstPost().getLastTouchedDate();
        for (Post post : posts) {
            if (post.getLastTouchedDate().isAfter(newTopicModificationDate.toInstant())) {
                newTopicModificationDate = post.getLastTouchedDate();
            }
        }
        modificationDate = newTopicModificationDate;
    }

    /**
     * @return flag og stickedness
     */
    public boolean isSticked() {
        return this.sticked;
    }

    /**
     * @param sticked a flag of stickedness for a topic
     */
    public void setSticked(boolean sticked) {
        this.sticked = sticked;
    }

    /**
     * @return flag og announcement
     */
    public boolean isAnnouncement() {
        return this.announcement;
    }

    /**
     * @param announcement a flag of announcement for a topic
     */
    public void setAnnouncement(boolean announcement) {
        this.announcement = announcement;
    }

    /**
     * Get count of post in topic.
     *
     * @return count of post
     */
    public int getPostCount() {
        return posts.size();
    }

    /**
     * @return topic page views
     */
    public int getViews() {
        return views;
    }

    /**
     * @param views topic page views
     */
    public void setViews(int views) {
        this.views = views;
    }

    /**
     * Get the poll for this topic.
     *
     * @return the poll for this topic
     */
    public Poll getPoll() {
        return poll;
    }

    /**
     * Set the poll for this topic.
     *
     * @param poll the poll for this topic
     */
    public void setPoll(Poll poll) {
        this.poll = poll;
    }

    /**
     * Gets the code review associated with the topic. Topic can't be a Poll 
     * or a simple topic if it has this not a null, same is vice versa - if 
     * the topic is already a Poll or it's a simple discussion-topic, it can't 
     * be a CR. In a most cases this association would probably be null. 
     * @return the codeReview
     */
    public CodeReview getCodeReview() {
        return codeReview;
    }

    /**
     * Set the code review for this topic
     * @param codeReview the codeReview to set
     */
    public void setCodeReview(CodeReview codeReview) {
        this.codeReview = codeReview;
    }

    /**
     * @param index last read post index in this topic for current user
     *              (0 means first post is the last read one)
     */
    public void setLastReadPostIndex(int index) {
        if (index < posts.size()) {
            lastReadPostIndex = index;
        } else {
            LOGGER.warn("Last read post index ({}) is bigger than last post index ({}) in the topic (TOPID ID: {})",
                new Object[] {index, posts.size() - 1, getId()});
            lastReadPostIndex = posts.size() - 1;
        }
    }
    
    public Integer getLastReadPostIndex() {
        return lastReadPostIndex;
    }

    /**
     * Returns first unread post for current user. If no unread post
     * information has been set explicitly this method will return
     * first topic's post id, considering all topic as unread.
     *
     * @return returns first unread post id for the current user
     */
    public Long getFirstUnreadPostId() {
        int index = (lastReadPostIndex == null) ? 0 : lastReadPostIndex + 1;
        return posts.get(index).getId();
    }

    /**
     * This method will return true if there are unread posts in that topic
     * for the current user. This state is NOT persisted and must be
     * explicitly set by calling  Topic.setLastReadPostIndex().
     * <p/>
     * If setter has not been called this method will always return no updates
     *
     * @return if current topic has posts still unread by the current user
     */
    public boolean isHasUpdates() {
        return (lastReadPostIndex == null) || (lastReadPostIndex + 1 < posts.size());
    }

    /**
     * Determines a existence the poll in the topic.
     *
     * @return <tt>true</tt>  if the poll exists
     *         <tt>false</tt>  if the poll doesn't exist
     */
    public boolean isHasPoll() {
        return poll != null;
    }

    /**
     * {@inheritDoc}
     */
    @DocumentId
    @Override
    public long getId() {
        return super.getId();
    }

    /**
     * {@inheritDoc}
     */
    public Set<JCUser> getSubscribers() {
        return subscribers;
    }

    /**
     * {@inheritDoc}
     */
    public void setSubscribers(Set<JCUser> subscribers) {
        this.subscribers = subscribers;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The target URL has the next format http://{forum root}/posts/{id}
     */
    @Override
    public String prepareUrlSuffix() {
        return URL_SUFFIX + getLastPost().getId();
    }

    /**
     * @return True if topic is closed
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * @param closed If true then topic set to closed, else to open
     */
    public void setClosed(boolean closed) {
        this.closed = closed;
    }
}
