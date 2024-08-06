# ---------------------------------------------------------------------------
# See the NOTICE file distributed with this work for additional
# information regarding copyright ownership.
#
# This is free software; you can redistribute it and/or modify it
# under the terms of the GNU Lesser General Public License as
# published by the Free Software Foundation; either version 2.1 of
# the License, or (at your option) any later version.
#
# This software is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this software; if not, write to the Free
# Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
# 02110-1301 USA, or see the FSF site: http://www.fsf.org.
# ---------------------------------------------------------------------------

# DO NOT EDIT - See: https://jetty.org/docs/index.html

[description]
Enables use of the apache implementation of JSP.

[environment]
ee8

[depend]
ee8-servlet
ee8-annotations

[ini]
eclipse.jdt.ecj.version?=3.38.0
ee8.jsp.impl.version?=9.0.90

[lib]
lib/ee8-apache-jsp/org.eclipse.jdt.ecj-${eclipse.jdt.ecj.version}.jar
lib/ee8-apache-jsp/org.mortbay.jasper.apache-el-${ee8.jsp.impl.version}.jar
lib/ee8-apache-jsp/org.mortbay.jasper.apache-jsp-${ee8.jsp.impl.version}.jar
lib/jetty-ee8-apache-jsp-${jetty.version}.jar
