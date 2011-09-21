/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */
package com.xpn.xwiki.web;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;

public class EditAction extends XWikiAction
{
    private static final Logger LOGGER = LoggerFactory.getLogger(EditAction.class);

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        String contentFromRequest = request.getParameter("content");
        String titleFromRequest = request.getParameter("title");
        XWikiDocument doc = context.getDoc();
        XWiki xwiki = context.getWiki();
        XWikiForm form = context.getForm();
        VelocityContext vcontext = (VelocityContext) context.get("vcontext");

        boolean hasTranslation = false;
        if (doc != context.get("tdoc")) {
            hasTranslation = true;
        }

        // We have to clone the context document because it is cached and the changes we are going to make are valid
        // only for the duration of the current request.
        doc = doc.clone();
        context.put("doc", doc);
        vcontext.put("doc", doc.newDocument(context));

        synchronized (doc) {
            EditForm peform = (EditForm) form;
            String parent = peform.getParent();
            if (parent != null) {
                doc.setParent(parent);
            }
            String creator = peform.getCreator();
            if (creator != null) {
                doc.setCreator(creator);
            }
            String defaultTemplate = peform.getDefaultTemplate();
            if (defaultTemplate != null) {
                doc.setDefaultTemplate(defaultTemplate);
            }
            String defaultLanguage = peform.getDefaultLanguage();
            if ((defaultLanguage != null) && !defaultLanguage.equals("")) {
                doc.setDefaultLanguage(defaultLanguage);
            }
            if (doc.isNew() && doc.getDefaultLanguage().equals("")) {
                doc.setDefaultLanguage(context.getWiki().getLanguagePreference(context));
            }

            String language = context.getWiki().getLanguagePreference(context);
            String languagefromrequest = context.getRequest().getParameter("language");
            languagefromrequest = (languagefromrequest == null) ? "" : languagefromrequest;
            String languagetoedit = languagefromrequest.equals("") ? language : languagefromrequest;

            // if no specific language is set or if it is "default" then we edit the current doc
            if ((languagetoedit == null) || (languagetoedit.equals("default"))) {
                languagetoedit = "";
            }
            // if the document is new then we edit it as the default
            // if the language to edit is the one of the default document then the language is the
            // default
            if (doc.isNew() || (doc.getDefaultLanguage().equals(languagetoedit))) {
                languagetoedit = "";
            }
            // if the doc does not exist in the language to edit and the language was not
            // explicitely set in the URL
            // then we edit the default doc, otherwise this can cause to create translations without
            // wanting it.
            if ((!hasTranslation) && languagefromrequest.equals("")) {
                languagetoedit = "";
            }

            XWikiDocument tdoc;
            if (languagetoedit.equals("")) {
                // In this case the created document is going to be the default document.
                tdoc = doc;
                if (doc.isNew()) {
                    doc.setDefaultLanguage(language);
                    doc.setLanguage("");
                }
            } else if ((!hasTranslation) && context.getWiki().isMultiLingual(context)) {
                // If the translated doc object is the same as the doc object this means the translated doc did not
                // exists so we need to create it.
                tdoc = new XWikiDocument(doc.getDocumentReference());
                tdoc.setLanguage(languagetoedit);
                tdoc.setContent(doc.getContent());
                tdoc.setSyntax(doc.getSyntax());
                tdoc.setAuthorReference(context.getUserReference());
                tdoc.setStore(doc.getStore());
            } else {
                // Edit existing translation. Clone the translated document object to be sure that the changes we are
                // going to make will last only for the duration of the current request.
                tdoc = ((XWikiDocument) context.get("tdoc")).clone();
            }

            // Check for edit section
            String sectionContent = "";
            int sectionNumber = 0;
            if (request.getParameter("section") != null && xwiki.hasSectionEdit(context)) {
                sectionNumber = NumberUtils.toInt(request.getParameter("section"));
                sectionContent = tdoc.getContentOfSection(sectionNumber);
            }
            vcontext.put("sectionNumber", new Integer(sectionNumber));

            try {
                tdoc.readFromTemplate(peform, context);
            } catch (XWikiException e) {
                if (e.getCode() == XWikiException.ERROR_XWIKI_APP_DOCUMENT_NOT_EMPTY) {
                    context.put("exception", e);
                    return "docalreadyexists";
                }
            }
            if (contentFromRequest != null) {
                tdoc.setContent(contentFromRequest);
            }
            if (titleFromRequest != null) {
                tdoc.setTitle(titleFromRequest);
            }
            if (StringUtils.isNotEmpty(sectionContent)) {
                if (contentFromRequest == null) {
                    tdoc.setContent(sectionContent);
                }
                String sectionTitle = doc.getDocumentSection(sectionNumber).getSectionTitle();
                if (titleFromRequest == null && StringUtils.isNotBlank(sectionTitle)) {
                    sectionTitle =
                        context.getMessageTool().get("core.editors.content.titleField.sectionEditingFormat",
                            tdoc.getRenderedTitle(Syntax.PLAIN_1_0, context), sectionNumber, sectionTitle);
                    tdoc.setTitle(sectionTitle);
                }
            }

            context.put("tdoc", tdoc);
            vcontext.put("tdoc", tdoc.newDocument(context));
            // XWiki applications that were previously using the inline action might still expect the cdoc (content
            // document) to be properly set on the context. Expose tdoc (translated document) as cdoc for backward
            // compatibility.
            context.put("cdoc", context.get("tdoc"));
            vcontext.put("cdoc", vcontext.get("tdoc"));

            /* Setup a lock */
            try {
                XWikiLock lock = tdoc.getLock(context);
                if ((lock == null) || (lock.getUserName().equals(context.getUser())) || (peform.isLockForce())) {
                    tdoc.setLock(context.getUser(), context);
                }
            } catch (Exception e) {
                e.printStackTrace();
                // Lock should never make XWiki fail
                // But we should log any related information
                LOGGER.error("Exception while setting up lock", e);
            }
        }

        // Make sure object property fields are displayed in edit mode.
        // See XWikiDocument#display(String, BaseObject, XWikiContext)
        // TODO: Revisit the display mode after the inline action is removed. Is the display mode still needed when
        // there is only one edit action?
        context.put("display", "edit");
        return "edit";
    }
}
