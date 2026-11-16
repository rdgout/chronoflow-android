package net.chronoflow.app

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

internal fun parseXml(path: String): Document =
    DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(path))

internal fun Element.children(name: String): List<Element> =
    (0 until childNodes.length).map { childNodes.item(it) }.filterIsInstance<Element>().filter { it.tagName == name }
