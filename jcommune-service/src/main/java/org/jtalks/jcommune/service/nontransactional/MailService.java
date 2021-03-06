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
package org.jtalks.jcommune.service.nontransactional;

import org.apache.velocity.app.VelocityEngine;
import org.jtalks.common.model.entity.Entity;
import org.jtalks.jcommune.model.entity.*;
import org.jtalks.jcommune.service.exceptions.MailingFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.ui.velocity.VelocityEngineUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * This service is focused on sending e-mail to forum users.
 * Notifications, confirmations or e-mail based subscriptions of a various
 * kind should use this service to perform e-mail sending.
 *
 * @author Evgeniy Naumenko
 * @author Eugeny Batov
 */
public class MailService {

    private final JavaMailSender mailSender;
    private final String from;
    private final VelocityEngine velocityEngine;
    private final MessageSource messageSource;
    private final JCommuneProperty notificationsEnabledProperty;

    private static final Logger LOGGER = LoggerFactory.getLogger(MailService.class);

    private static final String LOG_TEMPLATE = "Error occurred while sending updates of %s %d to %s";
    private static final String HTML_TEMPLATES_PATH = "org/jtalks/jcommune/service/templates/html/";
    private static final String PLAIN_TEXT_TEMPLATES_PATH = "org/jtalks/jcommune/service/templates/plaintext/";
    private static final String LINK = "link";
    private static final String LINK_LABEL = "linkLabel";
    private static final String USER = "user";
    private static final String NAME = "name";
    private static final String MESSAGE_SOURCE = "messageSource";
    private static final String RECIPIENT_LOCALE = "locale";
    private static final String NO_ARGS = "noArgs";

    /**
     * Creates a mailing service with a default template message autowired.
     * "From" property is essential. Please note, that this address should be valid email address
     * as most e-mail servers will reject e-mail if sender is not really correlated with
     * the letter's "from" value.
     *
     * @param sender spring mailing tool
     * @param from   blank message with "from" filed preset
     * @param engine engine for templating email notifications
     * @param source for resolving internationalization messages
     * @param notificationsEnabledProperty to check whether email notifications are enabled
     */
    public MailService(JavaMailSender sender,
            String from,
            VelocityEngine engine,
            MessageSource source,
            JCommuneProperty notificationsEnabledProperty) {
        this.mailSender = sender;
        this.from = from;
        this.velocityEngine = engine;
        this.messageSource = source;
        this.notificationsEnabledProperty = notificationsEnabledProperty;
    }

    /**
     * Sends a password recovery message for the user.
     * This method does not generate new password, just sends a message.
     *
     * @param user        a person we will send a mail
     * @param newPassword new user password to be placed in an email
     * @throws MailingFailedException when mailing failed
     */
    public void sendPasswordRecoveryMail(JCUser user, String newPassword) throws MailingFailedException {
        String urlSuffix = "/login";
        String url = this.getDeploymentRootUrl() + urlSuffix;
        String name = user.getUsername();
        Locale locale = user.getLanguage().getLocale();
        Map<String, Object> model = new HashMap<String, Object>();
        model.put(NAME, name);
        model.put("newPassword", newPassword);
        model.put(LINK, url);
        model.put(LINK_LABEL, getDeploymentRootUrlWithoutPort() + urlSuffix);
        model.put(RECIPIENT_LOCALE, locale);
        this.sendEmail(user.getEmail(), messageSource.getMessage("passwordRecovery.subject", new Object[]{}, locale),
                model, "passwordRecovery.vm");
        LOGGER.info("Password recovery email sent for {}", name);
    }


    /**
     * Sends update notification to user specified if
     * {@link SubscriptionAwareEntity} was updated, e.g. when some new
     * information were added to the subscribed entity. This method won't check if user
     * is subscribed to the particular notification or not.
     *
     * @param recipient a person to be notified about updates by email
     * @param entity    changed subscribed entity.
     */
    public void sendUpdatesOnSubscription(JCUser recipient, SubscriptionAwareEntity entity) {
        try {
            String urlSuffix = entity.prepareUrlSuffix();
            String url = this.getDeploymentRootUrl() + urlSuffix;
            Locale locale = recipient.getLanguage().getLocale();
            Map<String, Object> model = new HashMap<String, Object>();
            model.put(LINK, url);
            model.put(LINK_LABEL, getDeploymentRootUrlWithoutPort() + urlSuffix);
            sendEmailOnForumUpdates(recipient, model, locale, (Entity) entity);
        } catch (MailingFailedException e) {
            LOGGER.error(String.format(LOG_TEMPLATE,
                    entity.getClass().getCanonicalName(),
                    ((Entity) entity).getId(),
                    recipient.getUsername()));
        }
    }

    /**
     * Sends email on forum updates.
     *
     * @param recipient a person to be notified about updates by email
     * @param model     template params to be substituted in velocity template
     * @param locale    recipient locale
     * @throws MailingFailedException when mailing failed
     */
    private void sendEmailOnForumUpdates(JCUser recipient, Map<String, Object> model, Locale locale, Entity entity)
            throws MailingFailedException {
        model.put(USER, recipient);
        model.put(RECIPIENT_LOCALE, locale);
        String titleEntity = this.getTitleName(entity);
        this.sendEmail(recipient.getEmail(), messageSource.getMessage("subscriptionNotification.subject",
                new Object[]{}, locale) + titleEntity, model, "subscriptionNotification.vm");
    }

    /**
     * Sends notification to user about received private message.
     *
     * @param recipient a person to be notified about received private message by email
     * @param pm        private message itself
     */
    public void sendReceivedPrivateMessageNotification(JCUser recipient, PrivateMessage pm) {
        try {
            String urlSuffix = "/pm/inbox/" + pm.getId();
            String url = this.getDeploymentRootUrl() + urlSuffix;
            Locale locale = recipient.getLanguage().getLocale();
            Map<String, Object> model = new HashMap<String, Object>();
            model.put("recipient", recipient);
            model.put(LINK, url);
            model.put(LINK_LABEL, getDeploymentRootUrlWithoutPort() + urlSuffix);
            model.put(RECIPIENT_LOCALE, locale);
            this.sendEmail(recipient.getEmail(),
                    messageSource.getMessage("receivedPrivateMessageNotification.subject", new Object[]{}, locale),
                    model, "receivedPrivateMessageNotification.vm");
        } catch (MailingFailedException e) {
            LOGGER.error(String.format(LOG_TEMPLATE, "Private message", pm.getId(), recipient.getUsername()));
        }
    }

    /**
     * Sends email with a hyperlink to activate user account.
     *
     * @param recipient user to send activation mail to
     */
    public void sendAccountActivationMail(JCUser recipient) {
        try {
            String urlSuffix = "/user/activate/" + recipient.getUuid();
            String url = this.getDeploymentRootUrl() + urlSuffix;
            Locale locale = recipient.getLanguage().getLocale();
            Map<String, Object> model = new HashMap<String, Object>();
            model.put(NAME, recipient.getUsername());
            model.put(LINK, url);
            model.put(LINK_LABEL, getDeploymentRootUrlWithoutPort() + urlSuffix);
            model.put(RECIPIENT_LOCALE, locale);
            this.sendEmail(recipient.getEmail(), messageSource.getMessage("accountActivation.subject",
                    new Object[]{}, locale), model, "accountActivation.vm");
        } catch (MailingFailedException e) {
            LOGGER.error("Failed to sent activation mail for user: " + recipient.getUsername());
        }
    }

    /**
     * Sends email to topic starter that his or her topic was moved
     *
     * @param recipient user to send notification
     * @param topicId   id of relocated topic
     */
    public void sendTopicMovedMail(JCUser recipient, long topicId) {
        String urlSuffix = "/topics/" + topicId;
        String url = this.getDeploymentRootUrl() + urlSuffix;
        Locale locale = recipient.getLanguage().getLocale();
        Map<String, Object> model = new HashMap<String, Object>();
        model.put(NAME, recipient.getUsername());
        model.put(LINK, url);
        model.put(LINK_LABEL, getDeploymentRootUrlWithoutPort() + urlSuffix);
        model.put(RECIPIENT_LOCALE, locale);
        try {
            this.sendEmail(recipient.getEmail(), messageSource.getMessage("moveTopic.subject",
                    new Object[]{}, locale), model, "moveTopic.vm");
        } catch (MailingFailedException e) {
            LOGGER.error("Failed to sent activation mail for user: " + recipient.getUsername());
        }
    }
    
    /**
     * Send email notification to user when he was mentioned in forum.
     * Email notification will be sent only when notifications are enabled 
     * in forum, otherwise nothing will happen.
     * 
     * @param recipient mentioned user who will receive notification
     * @param postId id of post where user was mentioned
     */
    public void sendUserMentionedNotification(JCUser recipient, long postId) {
        if (notificationsEnabledProperty.booleanValue()) {
            String urlSuffix = "/posts/" + postId;
            String url = this.getDeploymentRootUrl() + urlSuffix;
            Locale locale = recipient.getLanguage().getLocale();
            Map<String, Object> model = new HashMap<String, Object>();
            model.put(NAME, recipient.getUsername());
            model.put(LINK, url);
            model.put(LINK_LABEL, getDeploymentRootUrlWithoutPort() + urlSuffix);
            model.put(RECIPIENT_LOCALE, locale);
            try {
                this.sendEmail(recipient.getEmail(), messageSource.getMessage("userMentioning.subject",
                        new Object[]{}, locale), model, "userMentioning.vm");
            } catch (MailingFailedException e) {
                LOGGER.error("Failed to sent activation mail for user: " + recipient.getUsername());
            }
        }
    }

    /**
     * Just a convenience method for message sending to encapsulate
     * boilerplate error handling code.
     *
     * @param to           destination email address
     * @param subject      message headline
     * @param model        template params to be substituted in velocity template
     * @param templateName template file name, like "template.vm"
     * @throws MailingFailedException exception with error message specified ic case of some error
     */
    private void sendEmail(String to, String subject, Map<String, Object> model, String templateName) throws
            MailingFailedException {
        try {
            model.put(MESSAGE_SOURCE, messageSource);
            model.put(NO_ARGS, new Object[]{});
            String plainText = this.mergePlainTextTemplate(templateName, model);
            String htmlText = this.mergeHtmlTemplate(templateName, model);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setFrom(from);
            helper.setSubject(subject);
            helper.setText(plainText, htmlText);
            mailSender.send(message);
        } catch (MailException e) {
            LOGGER.error("Mail sending failed", e);
            throw new MailingFailedException(e);
        } catch (MessagingException e) {
            LOGGER.error("Mail sending failed", e);
            throw new MailingFailedException(e);
        }
    }

    /**
     * Creates a html text message from templates and param given.
     * Template should be located in org/jtalks/jcommune/service/templates/html/
     *
     * @param templateName template file name, like "template.vm"
     * @param model        template params to be substituted in velocity template
     * @return html text message, ready to be sent
     */
    private String mergeHtmlTemplate(String templateName, Map<String, Object> model) {
        String path = HTML_TEMPLATES_PATH + templateName;
        return VelocityEngineUtils.mergeTemplateIntoString(velocityEngine, path, model);
    }

    /**
     * Creates a plain text message from templates and param given.
     * Template should be located in org/jtalks/jcommune/service/templates/plaintext/
     *
     * @param templateName template file name, like "template.vm"
     * @param model        template params to be substituted in velocity template
     * @return plain text message, ready to be sent
     */
    private String mergePlainTextTemplate(String templateName, Map<String, Object> model) {
        String path = PLAIN_TEXT_TEMPLATES_PATH + templateName;
        return VelocityEngineUtils.mergeTemplateIntoString(velocityEngine, path, model);
    }

    /**
     * @return current deployment root, e.g. "http://myhost.com:1234/mycoolforum"
     */
    private String getDeploymentRootUrl() {
        HttpServletRequest request = getServletRequest();
        return request.getScheme()
                + "://" + request.getServerName()
                + ":" + request.getServerPort()
                + request.getContextPath();
    }

    /**
     * Returns current deployment root without port for using as label link, for example.
     *
     * @return current deployment root without port, e.g. "http://myhost.com/mycoolforum"
     */
    private String getDeploymentRootUrlWithoutPort() {
        HttpServletRequest request = getServletRequest();
        return request.getScheme()
                + "://" + request.getServerName()
                + request.getContextPath();
    }

    /**
     * @return native {@link HttpServletRequest}
     */
    private HttpServletRequest getServletRequest() {
        RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
        return ((ServletRequestAttributes) attributes).getRequest();
    }

    /**
     * @param entity entity like "Branch/Topics"
     * @return title for Topic/Branch or "" if entity is not instanceof Topic/Branch
     */
    private String getTitleName(Entity entity) {
        if (entity instanceof Topic) {
            Topic topic = (Topic) entity;
            return ": " + topic.getTitle();
        } else if (entity instanceof Branch) {
            Branch branch = (Branch) entity;
            return ": " + branch.getName();
        } else if (entity instanceof CodeReview) {
            CodeReview codeReview = (CodeReview) entity;
            return ": " + codeReview.getTopic().getTitle();
        } else {
            return "";
        }
    }
}
