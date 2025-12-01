/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.manifest

import brut.androlib.res.xml.ResXmlUtils
import brut.xml.XmlUtils
import org.katastima.apkscanner.models.manifest.Action
import org.katastima.apkscanner.models.manifest.Category
import org.katastima.apkscanner.models.manifest.Data
import org.katastima.apkscanner.models.manifest.Feature
import org.katastima.apkscanner.models.manifest.Flag
import org.katastima.apkscanner.models.manifest.IntentFilter
import org.katastima.apkscanner.models.manifest.Permission
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

    fun pullApplicationFlags(file: File): List<Flag> {
        val applicationNodes = pullNodes(file, "/manifest/application")
        val applicationNode = applicationNodes.firstOrNull() ?: return emptyList()
        val applicationAttributes = applicationNode.attributes ?: return emptyList()
        if (applicationAttributes.length <= 0) return emptyList()

        val applicationFlags = mutableSetOf<Flag>()
        // TODO: clean this up.
        for (i in 0..applicationAttributes.length) {
            val attributeNode = applicationAttributes.item(i) ?: continue
            if (attributeNode.nodeName.isEmpty()) continue

            val flag = Flag(attributeNode.nodeName, attributeNode.nodeValue)
            applicationFlags.add(flag)
        }
        return applicationFlags.sortedBy { it.name }
    }

    fun pullIntentFilters(file: File): List<IntentFilter> {
        val applicationNodes = pullNodes(file, "/manifest/application")
        val applicationNode = applicationNodes.firstOrNull() ?: return emptyList()
        val applicationChildNodes = applicationNode.childNodes ?: return emptyList()
        if (applicationChildNodes.length <= 0) return emptyList()

        val nodeNamesToSearch = arrayOf(
            "activity",
            "activity-alias",
            "service",
            "receiver",
            "provider",
        )

        val intentFilters = mutableListOf<IntentFilter>()
        // TODO: clean this up.
        for (i in 0..applicationChildNodes.length) {
            val applicationChildNode = applicationChildNodes.item(i) ?: continue
            if (!nodeNamesToSearch.contains(applicationChildNode.nodeName)) continue
            if (applicationChildNode.childNodes == null) continue
            if (applicationChildNode.childNodes.length <= 0) continue

            for (i in 0..applicationChildNode.childNodes.length) {
                val intentFilterNode = applicationChildNode.childNodes.item(i) ?: continue
                if (intentFilterNode.nodeName != "intent-filter") continue
                if (intentFilterNode.childNodes == null) continue
                if (intentFilterNode.childNodes.length <= 0) continue

                val actionList = mutableListOf<Action>()
                val categoryList = mutableListOf<Category>()
                val dataList = mutableListOf<Data>()

                for (i in 0..intentFilterNode.childNodes.length) {
                    val intentFilterChildNode = intentFilterNode.childNodes.item(i) ?: continue
                    when (intentFilterChildNode.nodeName) {
                        "action" -> {
                            val action = pullAction(intentFilterChildNode) ?: continue
                            actionList.add(action)
                        }

                        "category" -> {
                            val category = pullCategory(intentFilterChildNode) ?: continue
                            categoryList.add(category)
                        }

                        "data" -> {
                            val data = pullData(intentFilterChildNode) ?: continue
                            dataList.add(data)
                        }

                        else -> continue
                    }
                }

                val intentFilter = IntentFilter(
                    actions = actionList,
                    categories = categoryList,
                    data = dataList,
                )
                intentFilters.add(intentFilter)
            }
        }
        return intentFilters
    }

    private fun pullAction(node: Node): Action? {
        val actionAttributes = node.attributes ?: return null
        val nameItem = actionAttributes.getNamedItem("android:name") ?: return null
        return Action(nameItem.nodeValue)
    }

    private fun pullCategory(node: Node): Category? {
        val categoryAttributes = node.attributes ?: return null
        val nameItem = categoryAttributes.getNamedItem("android:name") ?: return null
        return Category(nameItem.nodeValue)
    }

    private fun pullData(node: Node): Data? {
        val dataAttributes = node.attributes ?: return null
        return Data(
            scheme = dataAttributes.getNamedItem("android:scheme")?.nodeValue ?: "",
            host = dataAttributes.getNamedItem("android:host")?.nodeValue ?: "",
            port = dataAttributes.getNamedItem("android:port")?.nodeValue ?: "",
            path = dataAttributes.getNamedItem("android:path")?.nodeValue ?: "",
            pathPattern = dataAttributes.getNamedItem("android:pathPattern")?.nodeValue ?: "",
            pathPrefix = dataAttributes.getNamedItem("android:pathPrefix")?.nodeValue ?: "",
            pathSuffix = dataAttributes.getNamedItem("android:pathSuffix")?.nodeValue ?: "",
            pathAdvancedPattern = dataAttributes.getNamedItem("android:pathAdvancedPattern")?.nodeValue ?: "",
            mimeType = dataAttributes.getNamedItem("android:mimeType")?.nodeValue ?: "",
        )
    }


    fun pullPermissions(file: File): Set<Permission> {
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
        }.filter { it.name != "" }.toSet()
    }
}
