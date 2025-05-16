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
package org.xwiki.contrib.confluence.filter;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.internal.macros.AbstractMacroConverter;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Map;
import java.util.TreeMap;

/**
 * Converts to a block macro.
 * 
 * @version $Id$
 */
@Component
@Singleton
@Named("convertedtoblock")
public class FakeBlockMacroConverter extends AbstractMacroConverter
{
    @Override
    public String toXWikiId(String confluenceId, Map<String, String> confluenceParameters, String confluenceContent,
        boolean inline)
    {
        if (confluenceParameters.containsKey("pleasecrash")) {
            throw new RuntimeException("Crash requested, happy to oblige");
        }
        return "view-file";
    }

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        if (confluenceParameters.containsKey("pleasecollide")) {
            return new TreeMap<>(Map.of(
                "origpleasecollide", "fromConverter",
                "randomParam", "42"
            ));
        }

        return confluenceParameters;
    }
}
