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

import org.jtalks.common.model.permissions.GeneralPermission;
import org.jtalks.common.security.SecurityService;
import org.jtalks.jcommune.model.dao.PrivateMessageDao;
import org.jtalks.jcommune.model.dto.JCommunePageRequest;
import org.jtalks.jcommune.model.entity.JCUser;
import org.jtalks.jcommune.model.entity.JCommuneProperty;
import org.jtalks.jcommune.model.entity.PrivateMessage;
import org.jtalks.jcommune.model.entity.PrivateMessageStatus;
import org.jtalks.jcommune.service.PrivateMessageService;
import org.jtalks.jcommune.service.UserService;
import org.jtalks.jcommune.service.exceptions.NotFoundException;
import org.jtalks.jcommune.service.nontransactional.MailService;
import org.jtalks.jcommune.service.nontransactional.UserDataCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

/**
 * The implementation of PrivateMessageServices.
 *
 * @author Pavel Vervenko
 * @author Kirill Afonin
 * @author Max Malakhov
 */
public class TransactionalPrivateMessageService
        extends AbstractTransactionalEntityService<PrivateMessage, PrivateMessageDao> implements PrivateMessageService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SecurityService securityService;
    private final UserService userService;
    private final UserDataCacheService userDataCache;
    private final MailService mailService;
    private final JCommuneProperty sendingNotificationsEnabledProperty;

    /**
     * Creates the instance of service.
     *
     * @param pmDao           PrivateMessageDao
     * @param securityService for retrieving current user
     * @param userService     for getting user by name
     * @param userDataCache   service for cache for user data
     * @param mailService     for sending email notifications
     */
    public TransactionalPrivateMessageService(PrivateMessageDao pmDao,
                                              SecurityService securityService,
                                              UserService userService,
                                              UserDataCacheService userDataCache,
                                              MailService mailService,
                                              JCommuneProperty sendingNotificationsEnabledProperty) {
        super(pmDao);
        this.securityService = securityService;
        this.userService = userService;
        this.userDataCache = userDataCache;
        this.mailService = mailService;
        this.sendingNotificationsEnabledProperty = sendingNotificationsEnabledProperty;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<PrivateMessage> getInboxForCurrentUser(int page) {
        JCUser currentUser = userService.getCurrentUser();
        JCommunePageRequest pageRequest = new JCommunePageRequest(page,
                currentUser.getPageSize(), true);
        return this.getDao().getAllForUser(currentUser, pageRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<PrivateMessage> getOutboxForCurrentUser(int page) {
        JCUser currentUser = userService.getCurrentUser();
        JCommunePageRequest pageRequest = new JCommunePageRequest(page,
                currentUser.getPageSize(), true);
        return this.getDao().getAllFromUser(currentUser, pageRequest);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @PreAuthorize("hasPermission(#userFrom.id, 'USER', 'ProfilePermission.SEND_PRIVATE_MESSAGES')")
    public PrivateMessage sendMessage(String title, String body, JCUser recipient, JCUser userFrom) {

        PrivateMessage pm = new PrivateMessage(recipient, userFrom, title, body);
        pm.setRead(false);
        pm.setStatus(PrivateMessageStatus.SENT);
        this.getDao().saveOrUpdate(pm);

        userDataCache.incrementNewMessageCountFor(recipient.getUsername());

        securityService.createAclBuilder().grant(GeneralPermission.READ).to(recipient).on(pm).flush();
        securityService.createAclBuilder().grant(GeneralPermission.READ).to(userFrom).on(pm).flush();

        long pmId = pm.getId();
        if (sendingNotificationsEnabledProperty.booleanValue()) {
            mailService.sendReceivedPrivateMessageNotification(recipient, pm);
        }

        logger.debug("Private message to user {} was sent. Message id={}", recipient.getUsername(), pmId);

        return pm;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<PrivateMessage> getDraftsForCurrentUser(int page) {
        JCUser currentUser = userService.getCurrentUser();
        JCommunePageRequest pageRequest = new JCommunePageRequest(page,
                currentUser.getPageSize(), true);
        return this.getDao().getDraftsForUser(currentUser, pageRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PreAuthorize("hasPermission(#userFrom.id, 'USER', 'ProfilePermission.SEND_PRIVATE_MESSAGES')")
    public void saveDraft(long id, String recipient, String title, String body, JCUser userFrom) 
        throws NotFoundException {

        JCUser userTo = recipient != null ? userService.getByUsername(recipient) : null;

        PrivateMessage pm = new PrivateMessage(userTo, userFrom, title, body);
        pm.setId(id);
        pm.setStatus(PrivateMessageStatus.DRAFT);
        this.getDao().saveOrUpdate(pm);

        JCUser user = userService.getCurrentUser();
        securityService.createAclBuilder().grant(GeneralPermission.READ).to(user).on(pm).flush();
        securityService.createAclBuilder().grant(GeneralPermission.WRITE).to(user).on(pm).flush();

        logger.debug("Updated private message draft. Message id={}", pm.getId());

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int currentUserNewPmCount() {
        String username = securityService.getCurrentUserUsername();
        if (username == null) {
            return 0;
        }

        Integer count = userDataCache.getNewPmCountFor(username);
        if (count != null) {
            return count;
        }
        count = this.getDao().getNewMessagesCountFor(username);
        userDataCache.putNewPmCount(username, count);
        return count;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @PreAuthorize("hasPermission(#userFrom.id, 'USER', 'ProfilePermission.SEND_PRIVATE_MESSAGES')")
    public PrivateMessage sendDraft(long id, String title, String body,
                                    JCUser recipient, JCUser userFrom) throws NotFoundException {
        PrivateMessage pm = new PrivateMessage(recipient, userFrom, title, body);
        pm.setId(id);
        pm.setRead(false);
        pm.setStatus(PrivateMessageStatus.SENT);
        this.getDao().saveOrUpdate(pm);

        userDataCache.incrementNewMessageCountFor(recipient.getUsername());

        securityService.deleteFromAcl(pm);
        securityService.createAclBuilder().grant(GeneralPermission.READ).to(recipient).on(pm).flush();
        securityService.createAclBuilder().grant(GeneralPermission.READ).to(userFrom).on(pm).flush();

        long pmId = pm.getId();
        if (sendingNotificationsEnabledProperty.booleanValue()) {
            mailService.sendReceivedPrivateMessageNotification(recipient, pm);
        }

        logger.debug("Private message(was draft) to user {} was sent. Message id={}",
                recipient.getUsername(), pmId);

        return pm;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PreAuthorize("hasPermission(#id, 'PRIVATE_MESSAGE', 'GeneralPermission.READ')")
    public PrivateMessage get(Long id) throws NotFoundException {
        PrivateMessage pm = super.get(id);
        if (!hasCurrentUserAccessToPM(pm)) {
            throw new NotFoundException(String.format("current user has no right to read pm %s with id %d",
                    userService.getCurrentUser(), id));
        }
        if (this.ifMessageShouldBeMarkedAsRead(pm)) {
            pm.setRead(true);
            this.getDao().saveOrUpdate(pm);
            userDataCache.decrementNewMessageCountFor(pm.getUserTo().getUsername());
        }
        return pm;
    }

    /**
     * Checks if the private message should be marked as read.
     * The follwing conditions are checked:
     * <p>1. Current user is the recepient
     * <p>2. Message is not read already
     * <p>3. Message is not a draft
     *
     * @param pm private messag to be tested
     * @return if message should be marked as read
     */
    private boolean ifMessageShouldBeMarkedAsRead(PrivateMessage pm) {
        return userService.getCurrentUser().equals(pm.getUserTo())
                && !pm.isRead()
                && !pm.getStatus().equals(PrivateMessageStatus.DRAFT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String delete(List<Long> ids) throws NotFoundException {
        JCUser currentUser = userService.getCurrentUser();

        String result = "inbox";
        for (Long id : ids) {

            PrivateMessage message = this.get(id);

            switch (message.getStatus()) {
                case DRAFT:
                    this.getDao().delete(message);
                    result = "drafts";
                    break;
                case DELETED_FROM_INBOX:
                    this.getDao().delete(message);
                    result = "outbox";
                    break;
                case DELETED_FROM_OUTBOX:
                    this.getDao().delete(message);
                    result = "inbox";
                    break;
                case SENT:
                    if (currentUser.equals(message.getUserFrom())) {
                        message.setStatus(PrivateMessageStatus.DELETED_FROM_OUTBOX);
                        result = "outbox";
                    } else {
                        message.setStatus(PrivateMessageStatus.DELETED_FROM_INBOX);
                        result = "inbox";
                    }
                    break;
            }
        }
        return result;
    }

    private boolean hasCurrentUserAccessToPM(PrivateMessage privateMessage) throws NotFoundException {
        JCUser currentUser = userService.getCurrentUser();
        PrivateMessageStatus messageStatus = privateMessage.getStatus();

        if (currentUser.equals(privateMessage.getUserFrom()) &&
                (messageStatus.equals(PrivateMessageStatus.DELETED_FROM_OUTBOX))) {
            return false;
        }

        return !(currentUser.equals(privateMessage.getUserTo()) &&
                (messageStatus.equals(PrivateMessageStatus.DELETED_FROM_INBOX)));

    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @PreAuthorize("hasPermission(#senderId, 'USER', 'ProfilePermission.SEND_PRIVATE_MESSAGES')")
    public void checkPermissionsToSend(Long senderId) {
        logger.debug("Check permission to send private message for user - " + senderId);
    }
}
