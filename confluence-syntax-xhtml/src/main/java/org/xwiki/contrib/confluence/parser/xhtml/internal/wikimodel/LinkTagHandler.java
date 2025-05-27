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
 */
package org.xwiki.contrib.confluence.parser.xhtml.internal.wikimodel;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.contrib.confluence.parser.xhtml.ConfluenceReferenceConverter;
import org.xwiki.rendering.wikimodel.WikiParameter;
import org.xwiki.rendering.wikimodel.xhtml.handler.TagHandler;
import org.xwiki.rendering.wikimodel.xhtml.impl.TagContext;

/**
 * Handles links.
 * <p>
 * Example:
 * <p>
 * {@code
 * <ac:link ac:anchor="anchor">
 *   <ri:page ri:content-title="Page" ri:space-key="SPACE" />
 *   <ac:plain-text-link-body><![CDATA[label]] ></ac:plain-text-link-body>
 * </ac:link>
 * <ac:link ac:anchor="anchor">
 *   <ri:attachment ri:filename="file.png">
 *     <ri:page ri:content-title="xhtml" ri:space-key="SPACE" />
 *   </ri:attachment>
 *   <ac:plain-text-link-body><![CDATA[image1234.png]]></ac:plain-text-link-body>
 * </ac:link>
 * <ac:link ac:card-appearance="inline">
 *   <ri:page ri:content-title="Page with / in title" ri:version-at-save="1" />
 *   <ac:link-body>Page with / in title</ac:link-body>
 * </ac:link>
 * <ac:link ac:anchor="anchor">
 *   <ri:space ri:space-key="ds" />
 * </ac:link>
 * <ac:link>
 *   <ri:user ri:username="admin" />
 * </ac:link>
 * }
 *
 * @version $Id$
 * @since 9.0
 */
public class LinkTagHandler extends TagHandler implements ConfluenceTagHandler
{

    private static final String SELF = "@self";
    private static final String HOME = "@home";
    private static final String CURRENT_SPACE = "currentSpace()";

    private final ConfluenceReferenceConverter referenceConverter;

    /**
     * @param referenceConverter the reference converter to use
     */
    public LinkTagHandler(ConfluenceReferenceConverter referenceConverter)
    {
        super(false);
        this.referenceConverter = referenceConverter;
    }

    @Override
    protected void begin(TagContext context)
    {
        ConfluenceXHTMLWikiReference link = new ConfluenceXHTMLWikiReference();

        WikiParameter anchorParameter = context.getParams().getParameter("ac:anchor");

        if (anchorParameter != null) {
            link.setAnchor(anchorParameter.getValue());
        }

        context.getTagStack().pushStackParameter(CONFLUENCE_CONTAINER, link);
    }

    @Override
    protected void end(TagContext context)
    {
        ConfluenceXHTMLWikiReference link =
            (ConfluenceXHTMLWikiReference) context.getTagStack().popStackParameter(CONFLUENCE_CONTAINER);

        if (shouldNotIssueLink(link)) {
            return;
        }

        if (context.getTagStack().getStackParameter(AbstractMacroParameterTagHandler.IN_CONFLUENCE_PARAMETER) != null) {
            // We are in a confluence macro parameter, we put the link in the content instead of issuing a reference.
            saveLinkInParameter(context, link);
        } else {
            context.getScannerContext().onReference(link);
        }
    }

    private void saveLinkInParameter(TagContext context, ConfluenceXHTMLWikiReference link)
    {
        TagContext parentContext = context.getParentContext();
        if (StringUtils.isNotEmpty(parentContext.getContent())) {
            // ensure links are well separated
            parentContext.appendContent("\n");
        }

        if (handleViewFile(context, link, parentContext)) {
            return;
        }
        String ref = referenceConverter.convertDocumentReference(link.getSpace(), link.getPage());
        parentContext.appendContent(ref);
    }

    private boolean shouldNotIssueLink(ConfluenceXHTMLWikiReference link)
    {
        // If a user tag was inside the link tag, it was transformed into a mention macro.
        if (link.getUser() != null) {
            return true;
        }

        // Make sure to have a label for local anchors

        String anchor = link.getAnchor();
        String page = link.getPage();
        if (!StringUtils.isEmpty(anchor)) {
            if (link.getLabelXDOM() == null && StringUtils.isEmpty(link.getLabel())) {
                setPrettyLabel(link, anchor, page);
            }
        } else if (page == null && link.getSpace() == null && link.getUser() == null
            && link.getAttachment() == null) {
            // empty links are links to the current page
            link.setPageTitle(SELF);
        }
        return false;
    }

    private void setPrettyLabel(ConfluenceXHTMLWikiReference link, String anchor, String page)
    {
        String label = '#' + anchor;
        if (page != null && !SELF.equals(page)) {
            if (HOME.equals(page)) {
                label = convertSpaceForLabel(link.getSpace()) + label;
            } else {
                label = page + label;
            }
        }
        link.setLabel(label);
    }

    private String convertSpaceForLabel(String space)
    {
        if (StringUtils.isEmpty(space) || CURRENT_SPACE.equals(space)) {
            // current space
            String spaceRef = referenceConverter.convertSpaceReference(CURRENT_SPACE, false);
            if (StringUtils.isEmpty(spaceRef)) {
                return "";
            }
            int lastDot = spaceRef.lastIndexOf('.');
            if (lastDot == -1) {
                lastDot = 0;
            }
            return spaceRef.substring(lastDot);
        }

        return space;
    }

    private static boolean handleViewFile(TagContext context, ConfluenceXHTMLWikiReference link,
        TagContext parentContext)
    {
        if (MacroTagHandler.inViewFile(context)) {
            // If we are in a view-file macro, there might be a separate space parameter. This means we can't
            // resolve the page just yet.
            if (StringUtils.isEmpty(link.getSpace()) && StringUtils.isNotEmpty(link.getPage())) {
                // unfortunately, we can't assume the current space. Some macros define a space in a separate parameter,
                // and it would be wrong to resolve the reference here
                parentContext.appendContent(link.getPage());
            } else if (StringUtils.isEmpty(link.getPage()) && StringUtils.isNotEmpty(link.getSpace())) {
                // In case where the page and the space parameters are given separately, having the space resolved and
                // not the page is not workable
                parentContext.appendContent(link.getSpace());
            }
            // if both the space and the page are provided, resolving the reference is the cleanest way we
            // have to pass the information
            return true;
        }
        return false;
    }
}
