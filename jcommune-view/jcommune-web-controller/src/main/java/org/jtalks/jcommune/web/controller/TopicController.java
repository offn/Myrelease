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
package org.jtalks.jcommune.web.controller;

import org.jtalks.jcommune.model.entity.Branch;
import org.jtalks.jcommune.model.entity.JCUser;
import org.jtalks.jcommune.model.entity.Poll;
import org.jtalks.jcommune.model.entity.Post;
import org.jtalks.jcommune.model.entity.Topic;
import org.jtalks.jcommune.service.*;
import org.jtalks.jcommune.service.exceptions.NotFoundException;
import org.jtalks.jcommune.service.nontransactional.LocationService;
import org.jtalks.jcommune.web.dto.TopicDto;
import org.jtalks.jcommune.web.util.BreadcrumbBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.validation.Valid;

/**
 * Serves topic management web requests
 *
 * @author Kravchenko Vitaliy
 * @author Kirill Afonin
 * @author Teterin Alexandre
 * @author Max Malakhov
 * @author Evgeniy Naumenko
 * @author Eugeny Batov
 * @see Topic
 */
@Controller
public class TopicController {

    public static final String TOPIC_ID = "topicId";
    public static final String BRANCH_ID = "branchId";
    public static final String BREADCRUMB_LIST = "breadcrumbList";
    private static final String PAGING_ENABLED = "pagingEnabled";
    private static final String SUBMIT_URL = "submitUrl";
    private static final String TOPIC_VIEW = "topicForm";
    private static final String TOPIC_DTO = "topicDto";
    private static final String REDIRECT_URL = "redirect:/topics/";

    private TopicModificationService topicModificationService;
    private TopicFetchService topicFetchService;
    private PostService postService;
    private BranchService branchService;
    private LastReadPostService lastReadPostService;
    private UserService userService;
    private BreadcrumbBuilder breadcrumbBuilder;
    private LocationService locationService;
    private SessionRegistry sessionRegistry;

    /**
     * This method turns the trim binder on. Trim binder
     * removes leading and trailing spaces from the submitted fields.
     * So, it ensures, that all validations will be applied to
     * trimmed field values only.
     *
     * @param binder Binder object to be injected
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    /**
     * @param topicModificationService the object which provides actions on
     *                                 {@link org.jtalks.jcommune.model.entity.Topic} entity
     * @param postService              the object which provides actions on
     *                                 {@link org.jtalks.jcommune.model.entity.Post} entity
     * @param branchService            the object which provides actions on
     *                                 {@link org.jtalks.jcommune.model.entity.Branch} entity
     * @param lastReadPostService      to perform post-related actions
     * @param userService              to determine the current user logged in
     * @param breadcrumbBuilder        to create Breadcrumbs for pages
     * @param locationService          to track user location on forum (what page he is viewing now)
     * @param sessionRegistry          to obtain list of users currently online
     * @param topicFetchService        to load topics from a database
     */
    @Autowired
    public TopicController(TopicModificationService topicModificationService,
                           PostService postService,
                           BranchService branchService,
                           LastReadPostService lastReadPostService,
                           UserService userService,
                           BreadcrumbBuilder breadcrumbBuilder,
                           LocationService locationService,
                           SessionRegistry sessionRegistry,
                           TopicFetchService topicFetchService) {
        this.topicModificationService = topicModificationService;
        this.postService = postService;
        this.branchService = branchService;
        this.lastReadPostService = lastReadPostService;
        this.userService = userService;
        this.breadcrumbBuilder = breadcrumbBuilder;
        this.locationService = locationService;
        this.sessionRegistry = sessionRegistry;
        this.topicFetchService = topicFetchService;
    }

    /**
     * Shows page with form for new topic
     *
     * @param branchId {@link Branch} branch, where topic will be created
     * @return {@code ModelAndView} object with "newTopic" view, new {@link TopicDto} and branch id
     * @throws NotFoundException when branch was not found
     */
    @RequestMapping(value = "/topics/new", method = RequestMethod.GET)
    public ModelAndView showNewTopicPage(@RequestParam(BRANCH_ID) Long branchId) throws NotFoundException {
        Branch branch = branchService.get(branchId);
        Topic topic = new Topic();
        topic.setBranch(branch);
        topic.setPoll(new Poll());
        TopicDto dto = new TopicDto(topic);
        return new ModelAndView(TOPIC_VIEW)
                .addObject(TOPIC_DTO, dto)
                .addObject(BRANCH_ID, branchId)
                .addObject(SUBMIT_URL, "/topics/new?branchId=" + branchId)
                .addObject(BREADCRUMB_LIST, breadcrumbBuilder.getNewTopicBreadcrumb(branch));
    }

    /**
     * Create topic from data entered in form
     *
     * @param topicDto object with data from form
     * @param result   {@link BindingResult} validation result
     * @param branchId branch, where topic will be created
     * @return {@code ModelAndView} object which will be redirect to forum.html
     * @throws NotFoundException when branch not found
     */
    @RequestMapping(value = "/topics/new", method = RequestMethod.POST)
    public ModelAndView createTopic(@Valid @ModelAttribute TopicDto topicDto,
                                    BindingResult result,
                                    @RequestParam(BRANCH_ID) Long branchId) throws NotFoundException {
        Branch branch = branchService.get(branchId);
        if (result.hasErrors()) {
            return new ModelAndView(TOPIC_VIEW)
                    .addObject(BRANCH_ID, branchId)
                    .addObject(TOPIC_DTO, topicDto)
                    .addObject(SUBMIT_URL, "/topics/new?branchId=" + branchId)
                    .addObject(BREADCRUMB_LIST, breadcrumbBuilder.getForumBreadcrumb(branch));
        }
        Topic topic = topicDto.getTopic();
        topic.setBranch(branch);
        Topic createdTopic = topicModificationService.createTopic(topic, topicDto.getBodyText());

        lastReadPostService.markTopicAsRead(createdTopic);
        return new ModelAndView(REDIRECT_URL + createdTopic.getId());
    }

    /**
     * Delete topic
     *
     * @param topicId topic id, this is the topic which contains the first post which should be deleted
     * @return redirect to branch page
     * @throws NotFoundException when topic not found
     */
    @RequestMapping(value = "/topics/{topicId}", method = RequestMethod.DELETE)
    public ModelAndView deleteTopic(@PathVariable(TOPIC_ID) Long topicId) throws NotFoundException {
        Topic topic = topicFetchService.get(topicId);
        topicModificationService.deleteTopic(topic);
        return new ModelAndView("redirect:/branches/" + topic.getBranch().getId());
    }

    /**
     * Displays to user a list of messages from the topic with pagination
     *
     * @param topicId       the id of selected Topic
     * @param page          page
     * @param pagingEnabled if output data should be divided by pages
     * @return {@code ModelAndView}
     * @throws NotFoundException when topic or branch not found
     */
    @RequestMapping(value = "/topics/{topicId}", method = RequestMethod.GET)
    public ModelAndView showTopicPage(@PathVariable(TOPIC_ID) Long topicId,
                                      @RequestParam(value = "page", defaultValue = "1", required = false) int page,
                                      @RequestParam(value = PAGING_ENABLED, defaultValue = "true",
                                              required = false) Boolean pagingEnabled) throws NotFoundException {

        Topic topic = topicFetchService.get(topicId);
        topicFetchService.checkViewTopicPermission(topic.getBranch().getId());
        Page<Post> postsPage = postService.getPosts(topic, page, pagingEnabled);
        JCUser currentUser = userService.getCurrentUser();
        Integer lastReadPostIndex = lastReadPostService.getLastReadPostForTopic(topic);
        lastReadPostService.markTopicPageAsRead(topic, page, pagingEnabled);
        return new ModelAndView("postList")
                .addObject("viewList", locationService.getUsersViewing(topic))
                .addObject("usersOnline", sessionRegistry.getAllPrincipals())
                .addObject("postsPage", postsPage)
                .addObject("topic", topic)
                .addObject("subscribed", topic.getSubscribers().contains(currentUser))
                .addObject(BREADCRUMB_LIST, breadcrumbBuilder.getForumBreadcrumb(topic))
                .addObject("lastReadPost", lastReadPostIndex)
                .addObject("pagingEnabled", pagingEnabled);
    }

    /**
     * Shows edit topic page with form, populated with fields from topic.
     *
     * @param topicId the id of selected Topic
     * @return {@code ModelAndView}
     * @throws NotFoundException when topic or branch not found
     */
    @RequestMapping(value = "/topics/{topicId}/edit", method = RequestMethod.GET)
    public ModelAndView editTopicPage(@PathVariable(TOPIC_ID) Long topicId) throws NotFoundException {
        Topic topic = topicFetchService.get(topicId);
        if (topic.getCodeReview() != null) {
            throw new AccessDeniedException("Edit page for code review");
        }
        TopicDto topicDto = new TopicDto(topic);

        return new ModelAndView(TOPIC_VIEW)
                .addObject(BRANCH_ID, topic.getBranch().getId())
                .addObject(TOPIC_DTO, topicDto)
                .addObject(SUBMIT_URL, "/topics/" + topicId + "/edit")
                .addObject(BREADCRUMB_LIST, breadcrumbBuilder.getForumBreadcrumb(topic));
    }

    /**
     * Saves topic after edit topic form submission.
     *
     * @param topicDto Dto populated in form
     * @param result   validation result
     * @param topicId  the current topicId
     * @return {@code ModelAndView} with redirect to saved topic or the same page with validation errors, if any
     * @throws NotFoundException when topic or branch not found
     */
    @RequestMapping(value = "/topics/{topicId}/edit", method = RequestMethod.POST)
    public ModelAndView editTopic(@Valid @ModelAttribute TopicDto topicDto,
                                  BindingResult result,
                                  @PathVariable(TOPIC_ID) Long topicId) throws NotFoundException {
        Topic topic = topicFetchService.get(topicId);
        if (result.hasErrors()) {
            return new ModelAndView(TOPIC_VIEW)
                    .addObject(BRANCH_ID, topic.getBranch().getId())
                    .addObject(TOPIC_DTO, topicDto)
                    .addObject(SUBMIT_URL, "/topics/" + topicId + "/edit")
                    .addObject(BREADCRUMB_LIST, breadcrumbBuilder.getForumBreadcrumb(topic));
        }
        topicDto.fillTopic(topic);
        topicModificationService.updateTopic(topic, topicDto.getPoll());
        return new ModelAndView(REDIRECT_URL + topicId);
    }

    /**
     * Moves topic to another branch.
     *
     * @param topicId  id of moving topic
     * @param branchId id of target branch
     * @throws NotFoundException when topic or branch with given id not found
     */
    @RequestMapping(value = "/topics/move/json/{topicId}", method = RequestMethod.POST)
    @ResponseBody
    public void moveTopic(@PathVariable(TOPIC_ID) Long topicId,
                          @RequestParam(BRANCH_ID) Long branchId) throws NotFoundException {
        Topic topic = topicFetchService.get(topicId);
        topicModificationService.moveTopic(topic, branchId);
    }

    /**
     * Closes given topic or throws an exception if there is no such topic.
     * Closed topic is unavailable for posting until it's opened again.
     *
     * @param topicId identifies topic to be closed for further posting
     * @return redirection to the same topic
     * @throws NotFoundException if there is no topic for id given
     */
    @RequestMapping(value = "/topics/{topicId}/close")
    public String closeTopic(@PathVariable(TOPIC_ID) Long topicId) throws NotFoundException {
        Topic topic = topicFetchService.get(topicId);
        topicModificationService.closeTopic(topic);
        return REDIRECT_URL + topicId;
    }

    /**
     * Reopens topic for posting. If topic as opened already, does nothing
     *
     * @param topicId topic to be opened for posting
     * @return redirection to the same topic
     * @throws NotFoundException if there is no topic for id given
     */
    @RequestMapping(value = "/topics/{topicId}/open")
    public String openTopic(@PathVariable(TOPIC_ID) Long topicId) throws NotFoundException {
        Topic topic = topicFetchService.get(topicId);
        topicModificationService.openTopic(topic);
        return REDIRECT_URL + topicId;
    }
}
