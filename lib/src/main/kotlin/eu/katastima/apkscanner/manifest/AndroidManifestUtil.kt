/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.manifest

import brut.androlib.res.xml.ResXmlUtils
import brut.xml.XmlUtils
import eu.katastima.apkscanner.models.manifest.Feature
import eu.katastima.apkscanner.models.manifest.Permission
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.TransformerException
import javax.xml.xpath.XPathExpressionException

object AndroidManifestUtil {

    fun pullNodes(file: File, expression: String): List<Node> {
        try {
            val doc = XmlUtils.loadDocument(file, true)
            val nodes: NodeList = XmlUtils.evaluateXPath(doc, expression, NodeList::class.java) ?: return emptyList()

            val nodeList = arrayOfNulls<Node>(nodes.length)

            for (i in nodeList.indices) {
                nodeList[i] = nodes.item(i)
            }

            return nodeList.filterNotNull()
        } catch (_: IOException) {
            // Do nothing
        } catch (_: SAXException) {
            // Do nothing
        } catch (_: ParserConfigurationException) {
            // Do nothing
        } catch (_: XPathExpressionException) {
            // Do nothing
        }
        return emptyList()

    }

    fun pullManifestField(file: File, fieldName: String): String? {
        try {
            val doc = XmlUtils.loadDocument(file)
            val manifest = doc.firstChild
            val attrs = manifest.attributes

            val packageAttr = attrs.getNamedItem(fieldName)
            return packageAttr.nodeValue
        } catch (_: IOException) {
            // Do nothing
        } catch (_: SAXException) {
            // Do nothing
        } catch (_: ParserConfigurationException) {
            // Do nothing
        } catch (_: TransformerException) {
            // Do nothing
        }
        return null
    }

    fun pullPackageName(file: File): String? {
        return pullManifestField(file, "package")
    }

    fun pullApplicationLabel(file: File): String? {
        val applicationNodes = pullNodes(file, "/manifest/application")
        val applicationNode = applicationNodes.firstOrNull() ?: return null

        try {
            val label = applicationNode.attributes.getNamedItem("android:label").nodeValue
            return ResXmlUtils.pullValueFromStrings(file.parentFile, label)
        } catch (_: Exception) {
            return null
        }
    }

    fun pullFeatures(file: File): List<Feature> {
        val usesFeatureNodes = pullNodes(file, "/manifest/uses-feature")
        return usesFeatureNodes.map {
            try {
                val name = it.attributes.getNamedItem("android:name").nodeValue
                val required = it.attributes.getNamedItem("android:required")?.nodeValue?.equals("true", true) ?: true
                Feature(name, required)
            } catch (_: Exception) {
                Feature("")
            }
        }.filter { it.name != "" }
    }

    fun pullPermissions(file: File): List<Permission> {
        val usesPermissionNodes = pullNodes(file, "/manifest/uses-permission")
        val permissionNodes = pullNodes(file, "/manifest/permission")
        return (usesPermissionNodes + permissionNodes).map {
            try {
                val name = it.attributes.getNamedItem("android:name").nodeValue
                val minSdkVersion = it.attributes.getNamedItem("android:minSdkVersion")?.nodeValue?.toIntOrNull() ?: -1
                val maxSdkVersion = it.attributes.getNamedItem("android:maxSdkVersion")?.nodeValue?.toIntOrNull() ?: -1
                Permission(name, minSdkVersion, maxSdkVersion)
            } catch (_: Exception) {
                Permission("")
            }
        }.filter { it.name != "" }
    }
}
