package org.jetbrains.plugins.scala.lang.parser.parsing.types

import com.intellij.lang.PsiBuilder, org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.parser.ScalaElementTypes
import org.jetbrains.plugins.scala.lang.parser.bnf.BNF
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.tree.IElementType
import org.jetbrains.plugins.scala.lang.parser.util.ParserUtils
import org.jetbrains.plugins.scala.lang.lexer.ScalaElementType
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes._
import ScalaElementTypes._

/** 
* Created by IntelliJ IDEA.
* User: Alexander.Podkhalyuz
* Date: 15.02.2008
* Time: 15:47:11
* To change this template use File | Settings | File Templates.
*/

/*
 * StableId ::= id
 *            | Path '.' id
 *            | [id '.'] 'super' [ClassQualifier] '.' id
 */

object StableId extends ParserNode {

  def parse(builder: PsiBuilder, element: ScalaElementType): Boolean = parse(builder, false, element)

  def parse(builder: PsiBuilder, forImport: Boolean, element: ScalaElementType): Boolean = {
    val marker = builder.mark
    builder.getTokenType match {
      case ScalaTokenTypes.tIDENTIFIER => {
        builder.advanceLexer
        if (stopAtImportEnd(builder, forImport)) {
          marker.done(element)
          return true
        } else if (builder.getTokenType == tDOT) {
          val nm = marker.precede
          marker.done(element)
          builder.advanceLexer
          builder.getTokenType match {
            case ScalaTokenTypes.tIDENTIFIER => return parseQualId(builder, nm, element, forImport)
            case ScalaTokenTypes.kTHIS => return parseThisReference(builder, nm, element, forImport)
            case ScalaTokenTypes.kSUPER => return parseSuperReference(builder, nm, element, forImport)
            case _ => {
              builder error ErrMsg("identifier.expected")
              nm.done(element)
              return true
            }
          }
        }
        marker.done(element)
        return true
      }
      case ScalaTokenTypes.kTHIS => return parseThisReference(builder, marker, element, forImport)
      case ScalaTokenTypes.kSUPER => return parseSuperReference(builder, marker, element, forImport)
      case _ => {
        marker.drop
        return false
      }

    }
  }

  def parseThisReference(builder: PsiBuilder, marker: PsiBuilder.Marker, element: ScalaElementType, forImport: Boolean): Boolean = {
    val nm = marker.precede()
    builder.advanceLexer
    if (builder.getTokenType != tDOT) {
      builder.error(ErrMsg("dot.expected"))
      marker.done(THIS_REFERENCE)
      return true
    }
    marker.done(THIS_REFERENCE)
    return parseEndIdentifier(builder, nm, element, forImport)
  }

  def parseSuperReference(builder: PsiBuilder, marker: PsiBuilder.Marker, element: ScalaElementType, forImport: Boolean): Boolean = {
    val nm = marker.precede()
    builder.advanceLexer
    if (builder.getTokenType != tDOT && builder.getTokenType != tLSQBRACKET) {
      builder.error(ErrMsg("dot.or.cq.expected"))
      nm.drop
      marker.done(SUPER_REFERENCE)
      return true
    }
    parseClassQualifier(builder)
    marker.done(SUPER_REFERENCE)
    return parseEndIdentifier(builder, nm, element, forImport)
  }

  def parseClassQualifier(builder: PsiBuilder): Unit = {
    if (builder.getTokenType != tLSQBRACKET) return
    builder.advanceLexer
    if (builder.getTokenType != tIDENTIFIER) {
      builder.error(ErrMsg("identifier.expected"))
    }
    else {
      builder.advanceLexer
    }

    if (builder.getTokenType != tRSQBRACKET) {
      builder.error(ErrMsg("rsqbracket.expected"))
    }
    else {
      builder.advanceLexer
    }
  }


  // For endings of 'this' and 'super' references
  def parseEndIdentifier(builder: PsiBuilder, nm: PsiBuilder.Marker, element: ScalaElementType, forImport: Boolean): Boolean = {
    builder.advanceLexer
    if (builder.getTokenType != tIDENTIFIER) {
      builder.error(ErrMsg("identifier.expected"))
      nm.done(element)
      return true
    }
    builder.advanceLexer()
    if (stopAtImportEnd(builder, forImport)) {
      nm.done(element)
      return true
    } else if (builder.getTokenType == tDOT) {
      val nm1 = nm.precede()
      nm.done(element)
      builder.advanceLexer
      parseQualId(builder, nm1, element, forImport)
    } else {
      nm.done(element)
      return true
    }
  }

  // Begins from next id (not form dot)
  def parseQualId(builder: PsiBuilder, marker: PsiBuilder.Marker, element: ScalaElementType, forImport: Boolean): Boolean = {
    if (builder.getTokenType != tIDENTIFIER) {
      builder.error(ErrMsg("identifier.expected"))
      marker.done(element)
      return true
    }
    builder.advanceLexer // ate identifier
    if (stopAtImportEnd(builder, forImport)){
      marker.done(element)
      true
    } else if (builder.getTokenType == tDOT) {
      val nm = marker.precede
      marker.done(element)
      builder.advanceLexer // ate dot
      parseQualId(builder, nm, element, forImport)
    } else {
      marker.done(element)
      true
    }
  }

  def stopAtImportEnd(builder: PsiBuilder, forImport: Boolean) = forImport && isImportEnd(builder)

  def isImportEnd(builder: PsiBuilder): Boolean = {
    lookAhead(builder, tDOT, tUNDER) || lookAhead(builder, tDOT, tLBRACE)
  }

}