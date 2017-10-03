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

            <xsl:when test="@tag='00100010'
                         or @tag='00100020'
                         or @tag='00101010'
                         or @tag='00101020'
                         or @tag='00101030'
                         or @tag='00080050'
                         or @tag='00080020'
                         or @tag='00080061'
                         or @tag='00081030'
                         or @tag='00201208'
                         or @tag='0020000D'
                         or @tag='00080080'
                         or @tag='0020000E'
                         or @tag='0008103E'
                         or @tag='00080060'
                         or @tag='00200011'
                         or @tag='00080021'
                         or @tag='00080031'
                         or @tag='00080008'
                         or @tag='00080016'
                         or @tag='00080018'
                         or @tag='00200013'
                         or @tag='00200032'
                         or @tag='00200037'
                         or @tag='00200052'
                         or @tag='00201041'
                         or @tag='00280002'
                         or @tag='00280004'
                         or @tag='00280006'
                         or @tag='00280010'
                         or @tag='00280011'
                         or @tag='00280030'
                         or @tag='00280034'
                         or @tag='00280100'
                         or @tag='00280101'
                         or @tag='00280102'
                         or @tag='00280103'
                         or @tag='00280106'
                         or @tag='00280107'
                         or @tag='00281050'
                         or @tag='00281051'
                         or @tag='00281052'
                         or @tag='00281053'
                         or @tag='00281054'
                         or @tag='00200062'
                         or @tag='00185101'
                         or @tag='0008002A'
                         or @tag='00280008'
                         or @tag='00280009'
                         or @tag='00181063'
                         or @tag='00181065'
                         or @tag='00180050'
                         or @tag='00282110'
                         or @tag='00282111'
                         or @tag='00282112'
                         or @tag='00282114'
                         or @tag='00180086'
                         or @tag='00180010'">

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