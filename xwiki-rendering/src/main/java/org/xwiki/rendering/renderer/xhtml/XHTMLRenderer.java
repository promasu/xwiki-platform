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
package org.xwiki.rendering.renderer.xhtml;

import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.rendering.configuration.RenderingConfiguration;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.SectionLevel;
import org.xwiki.rendering.renderer.AbstractPrintRenderer;
import org.xwiki.rendering.renderer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.Renderer;
import org.xwiki.rendering.renderer.WikiPrinter;

/**
 * Generates XHTML from a {@link org.xwiki.rendering.block.XDOM} object being traversed.
 * 
 * @version $Id$
 * @since 1.5M2
 */
public class XHTMLRenderer extends AbstractPrintRenderer
{
    private DocumentAccessBridge documentAccessBridge;

    private RenderingConfiguration configuration;

    /**
     * A temporary service offering methods manipulating XWiki Documents that are needed to output the correct XHTML.
     * For example this is used to verify if a document exists when computing the HREF attribute for a link. It's
     * temporary because the current Document services have not yet been rewritten with the new architecture. This
     * bridge allows us to be independent of the XWiki Core module, thus preventing a cyclic dependency.
     */
    private XHTMLLinkRenderer linkRenderer;

    private XHTMLIdGenerator idGenerator;

    /**
     * Used to save the original Printer when we redirect all outputs to a new Printer to compute a section title. We
     * need to do this since the XHTML we generate for a section title contains a unique id that we generate based on
     * the section title and the events for the section title are generated after the beginSection() event.
     */
    private WikiPrinter originalPrinter;

    /**
     * The temporary Printer used to redirect all outputs when computing the section title.
     * 
     * @see #originalPrinter
     */
    private WikiPrinter sectionTitlePrinter;

    /**
     * @param printer the object to which to write the XHTML output to
     * @param documentAccessBridge see {@link #documentAccessBridge}
     */
    public XHTMLRenderer(WikiPrinter printer, DocumentAccessBridge documentAccessBridge,
        RenderingConfiguration configuration)
    {
        super(printer);
        this.documentAccessBridge = documentAccessBridge;
        this.linkRenderer = new XHTMLLinkRenderer(documentAccessBridge, configuration);
        this.configuration = configuration;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Renderer#beginDocument()
     */
    public void beginDocument()
    {
        // Use a new generator for each document being processed since the id generator is stateful and
        // remembers the generated ids.
        this.idGenerator = new XHTMLIdGenerator();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Renderer#endDocument()
     */
    public void endDocument()
    {
        // Don't do anything
    }

    /**
     * {@inheritDoc}
     * 
     * @see Renderer#beginFormat(org.xwiki.rendering.listener.Format)
     */
    public void beginFormat(Format format)
    {
        switch (format) {
            case BOLD:
                print("<strong>");
                break;
            case ITALIC:
                print("<em class=\"italic\">");
                break;
            case STRIKEDOUT:
                print("<del>");
                break;
            case UNDERLINED:
                // Note: XHTML has deprecated the usage of <u> in favor of style sheets.
                // Thus we use a class instead so that it can be styled using CSS.
                print("<span class=\"underline\">");
                break;
            case SUPERSCRIPT:
                print("<sup>");
                break;
            case SUBSCRIPT:
                print("<sub>");
                break;
            case MONOSPACE:
                print("<tt>");
                break;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Renderer#endFormat(org.xwiki.rendering.listener.Format)
     */
    public void endFormat(Format format)
    {
        switch (format) {
            case BOLD:
                print("</strong>");
                break;
            case ITALIC:
                print("</em>");
                break;
            case STRIKEDOUT:
                print("</del>");
                break;
            case UNDERLINED:
                // Note: XHTML has deprecated the usage of <u> in favor of style sheets.
                // Thus we use a class instead so that it can be styled using CSS.
                print("</span>");
                break;
            case SUPERSCRIPT:
                print("</sup>");
                break;
            case SUBSCRIPT:
                print("</sub>");
                break;
            case MONOSPACE:
                print("</tt>");
                break;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see Renderer#beginParagraph(java.util.Map)
     */
    public void beginParagraph(Map<String, String> parameters)
    {
        StringBuffer buffer = new StringBuffer("<p");
        buffer.append(serializeParameters(parameters));
        buffer.append('>');
        print(buffer.toString());
    }

    /**
     * {@inheritDoc}
     * 
     * @see Renderer#endParagraph(java.util.Map)
     */
    public void endParagraph(Map<String, String> parameters)
    {
        print("</p>");
    }

    /**
     * {@inheritDoc}
     * 
     * @see Renderer#onLineBreak()
     */
    public void onLineBreak()
    {
        print("<br/>");
    }

    /**
     * {@inheritDoc}
     * 
     * @see Renderer#onNewLine()
     */
    public void onNewLine()
    {
        // Voluntarily do nothing since we want the same behavior as HTML.
    }

    /**
     * {@inheritDoc}
     * 
     * @see Renderer#onLink(Link)
     */
    public void onLink(Link link)
    {
        print(this.linkRenderer.renderLink(link));
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.renderer.Renderer#onInlineMacro(String, java.util.Map, String)
     */
    public void onInlineMacro(String name, Map<String, String> parameters, String content)
    {
        // Do nothing since macro output depends on Macro execution which transforms the macro
        // into a set of other events.
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.renderer.Renderer#onStandaloneMacro(String, java.util.Map, String)
     */
    public void onStandaloneMacro(String name, Map<String, String> parameters, String content)
    {
        // Do nothing since macro output depends on Macro execution which transforms the macro
        // into a set of other events.
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#beginSection(org.xwiki.rendering.listener.SectionLevel)
     */
    public void beginSection(SectionLevel level)
    {
        // Don't output anything yet since we need the section title to generate the unique XHTML id attribute.
        // Thus we're doing the output in the endSection() event.

        // Redirect all output to our writer
        this.originalPrinter = getPrinter();
        this.sectionTitlePrinter = new DefaultWikiPrinter();
        this.setPrinter(this.sectionTitlePrinter);
    }

    private void processBeginSection(SectionLevel level, String sectionTitle)
    {
        int levelAsInt = level.getAsInt();
        print("<h" + levelAsInt + " id=\"" + this.idGenerator.generateUniqueId(sectionTitle) + "\">");
        // We generate a span so that CSS rules have a hook to perform some magic that wouldn't work on just a H
        // element. Like some IE6 magic and others.
        print("<span>");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#endSection(org.xwiki.rendering.listener.SectionLevel)
     */
    public void endSection(SectionLevel level)
    {
        String sectionTitle = this.sectionTitlePrinter.toString();
        setPrinter(this.originalPrinter);
        processBeginSection(level, sectionTitle);
        print(sectionTitle);

        int levelAsInt = level.getAsInt();
        print("</span>");
        print("</h" + levelAsInt + ">");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#onWord(String)
     */
    public void onWord(String word)
    {
        print(word);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#onSpace()
     */
    public void onSpace()
    {
        print(" ");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#onSpecialSymbol(String)
     */
    public void onSpecialSymbol(String symbol)
    {
        print(StringEscapeUtils.escapeHtml(symbol));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#onEscape(String)
     */
    public void onEscape(String escapedString)
    {
        // Print characters as is, except for special characters which need to be HTML-escaped.
        onSpecialSymbol(escapedString);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#beginList(org.xwiki.rendering.listener.ListType)
     */
    public void beginList(ListType listType)
    {
        if (listType == ListType.BULLETED) {
            print("<ul class=\"star\">");
        } else {
            print("<ol>");
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#beginListItem()
     */
    public void beginListItem()
    {
        print("<li>");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#endList(org.xwiki.rendering.listener.ListType)
     */
    public void endList(ListType listType)
    {
        if (listType == ListType.BULLETED) {
            print("</ul>");
        } else {
            print("</ol>");
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#endListItem()
     */
    public void endListItem()
    {
        print("</li>");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#beginXMLElement(String, java.util.Map)
     */
    public void beginXMLElement(String name, Map<String, String> attributes)
    {
        print("<" + name);
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            print(" " + entry.getKey() + "=\"" + entry.getValue() + "\"");
        }
        print(">");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#endXMLElement(String, java.util.Map)
     */
    public void endXMLElement(String name, Map<String, String> attributes)
    {
        print("</" + name + ">");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#beginMacroMarker(String, java.util.Map, String)
     */
    public void beginMacroMarker(String name, Map<String, String> parameters, String content)
    {
        // Ignore macro markers, nothing to do.
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#endMacroMarker(String, java.util.Map, String)
     */
    public void endMacroMarker(String name, Map<String, String> parameters, String content)
    {
        // Ignore macro markers, nothing to do.
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#onId(String)
     */
    public void onId(String name)
    {
        print("<a id=\"" + name + "\" name=\"" + name + "\"></a>");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#onHorizontalLine()
     */
    public void onHorizontalLine()
    {
        print("<hr/>");
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.renderer.Renderer#onVerbatimInline(String)
     */
    public void onVerbatimInline(String protectedString)
    {
        // Note: We generate a tt element rather than a pre element since pre elements cannot be located inside
        // paragraphs for example. There also no tag in XHTML that has a semantic of preserving inline content so
        // tt is the closed to pre for inline.
        // The class is what is expected by wikimodel to understand the tt as meaning a verbatim and not a Monospace
        // element.
        print("<tt class=\"wikimodel-verbatim\">" + protectedString + "</tt>");
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.renderer.Renderer#onVerbatimStandalone(String)
     */
    public void onVerbatimStandalone(String protectedString)
    {
        print("<pre>" + protectedString + "</pre>");
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.renderer.Renderer#onEmptyLines(int)
     */
    public void onEmptyLines(int count)
    {
        // TODO: Don't know yet how to represent this in HTML...
    }

    public void beginDefinitionList()
    {
        print("<dl>");
    }

    public void endDefinitionList()
    {
        print("</dl>");
    }

    public void beginDefinitionTerm()
    {
        print("<dt>");
    }

    public void beginDefinitionDescription()
    {
        print("<dd>");
    }

    public void endDefinitionTerm()
    {
        print("</dt>");
    }

    public void endDefinitionDescription()
    {
        print("</dd>");
    }

    public void beginQuotation(Map<String, String> parameters)
    {
        StringBuffer buffer = new StringBuffer("<blockquote");
        buffer.append(serializeParameters(parameters));
        buffer.append('>');
        print(buffer.toString());
    }

    public void endQuotation(Map<String, String> parameters)
    {
        print("</blockquote>");
    }

    public void beginQuotationLine()
    {
        // Nothing to do
    }

    public void endQuotationLine()
    {
        // Nothing to do
    }

    private StringBuffer serializeParameters(Map<String, String> parameters)
    {
        StringBuffer buffer = new StringBuffer();
        if (!parameters.isEmpty()) {
            for (String key: parameters.keySet()) {
                buffer.append(' ').append(key).append('=').append('\"').append(parameters.get(key)).append('\"');
            }
        }
        return buffer;
    }
}
