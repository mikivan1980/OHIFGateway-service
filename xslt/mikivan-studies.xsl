<?xml version="1.1" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="text"/>

    <xsl:variable name="HEX0">0123456769ABCDEF</xsl:variable>
    <xsl:variable name="HEX1">123456769ABCDEF0</xsl:variable>


    <xsl:template match="/NativeDicomModel">
        <xsl:text>{</xsl:text>

        <xsl:apply-templates mode="mikivan" select="DicomAttribute"/>

        <xsl:text>},</xsl:text>
    </xsl:template>



    <xsl:template match="DicomAttribute" mode="mikivan">

        <xsl:choose>

            <xsl:when test="@tag='0020000D'
                         or @tag='00080020'
                         or @tag='00080030'
                         or @tag='00080050'
                         or @tag='00080090'
                         or @tag='00100010'
                         or @tag='00100020'
                         or @tag='00100030'
                         or @tag='00100040'
                         or @tag='00200010'
                         or @tag='00201206'
                         or @tag='00201208'
                         or @tag='00081030'
                         or @tag='00080060'
                         or @tag='00080061'">

                <xsl:if test="position()>1">,</xsl:if>

                <xsl:text>"</xsl:text>
                <xsl:value-of select="@tag"/>
                <xsl:text>"</xsl:text>

                <xsl:text>:{"vr":"</xsl:text>
                <xsl:value-of select="@vr"/>
                <xsl:text>",</xsl:text>


                <xsl:text>"Value":["</xsl:text>

                <xsl:apply-templates mode="mikivan" select="Value" />
                <xsl:apply-templates mode="mikivan" select="PersonName" />

                <xsl:text>"]}</xsl:text>

            </xsl:when>

            <xsl:otherwise></xsl:otherwise>

        </xsl:choose>


    </xsl:template>


    <xsl:template match="Value" mode="mikivan">
         <xsl:value-of select="text()"/>
    </xsl:template>

    <xsl:template match="PersonName" mode="mikivan">
        <xsl:apply-templates select="*"/>
    </xsl:template>

</xsl:stylesheet>