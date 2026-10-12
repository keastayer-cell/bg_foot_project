#!/usr/bin/env python3
"""Сборка и проверка актуальности PDF из единственного Markdown-источника."""
import argparse
import hashlib
import os
import re
from pathlib import Path
from xml.sax.saxutils import escape

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'docs/Руководство-по-продукту.md'
OUTPUT = ROOT / 'docs/Руководство-по-продукту.pdf'


def font_paths():
    candidates = [
        (os.environ.get('GUIDE_FONT_REGULAR'), os.environ.get('GUIDE_FONT_BOLD')),
        ('/System/Library/Fonts/Supplemental/Arial.ttf', '/System/Library/Fonts/Supplemental/Arial Bold.ttf'),
        ('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'),
    ]
    for regular, bold in candidates:
        if regular and bold and Path(regular).is_file() and Path(bold).is_file():
            return regular, bold
    raise SystemExit('Нужны шрифты с кириллицей: Arial/DejaVu Sans или GUIDE_FONT_REGULAR и GUIDE_FONT_BOLD.')


def build(source, digest):
    from reportlab.lib import colors
    from reportlab.lib.enums import TA_LEFT
    from reportlab.lib.pagesizes import A4
    from reportlab.lib.styles import ParagraphStyle
    from reportlab.pdfbase import pdfmetrics
    from reportlab.pdfbase.ttfonts import TTFont
    from reportlab.platypus import BaseDocTemplate, Frame, PageTemplate, Paragraph, Spacer, PageBreak, LongTable, TableStyle, KeepTogether
    from reportlab.platypus.tableofcontents import TableOfContents

    version = re.search(r'^Версия документа: (.+)$', source, re.M).group(1)
    date = re.search(r'^Дата актуальности: (.+)$', source, re.M).group(1)
    regular, bold = font_paths()
    pdfmetrics.registerFont(TTFont('Guide', regular))
    pdfmetrics.registerFont(TTFont('GuideBold', bold))
    pdfmetrics.registerFontFamily('Guide', normal='Guide', bold='GuideBold', italic='Guide', boldItalic='GuideBold')
    ink = colors.HexColor('#19243A')
    green = colors.HexColor('#12745A')
    muted = colors.HexColor('#637082')
    width, height = A4
    styles = {
        'body': ParagraphStyle('body', fontName='Guide', fontSize=10, leading=15, textColor=ink, spaceAfter=8),
        'h1': ParagraphStyle('chapter', fontName='GuideBold', fontSize=21, leading=27, textColor=green, spaceAfter=18, keepWithNext=True),
        'h2': ParagraphStyle('section', fontName='GuideBold', fontSize=13, leading=18, textColor=ink, spaceBefore=10, spaceAfter=8, keepWithNext=True),
        'cell': ParagraphStyle('cell', fontName='Guide', fontSize=8.4, leading=12, textColor=ink),
        'head': ParagraphStyle('head', fontName='GuideBold', fontSize=8.4, leading=12, textColor=colors.white),
        'bullet': ParagraphStyle('bullet', fontName='Guide', fontSize=10, leading=15, textColor=ink, leftIndent=12, firstLineIndent=-12, spaceAfter=6),
    }

    def inline(value):
        text = escape(value)
        text = re.sub(r'\*\*(.+?)\*\*', r'<b>\1</b>', text)
        return re.sub(r'`([^`]+)`', r'\1', text)

    class GuideDoc(BaseDocTemplate):
        def afterFlowable(self, flowable):
            if isinstance(flowable, Paragraph) and flowable.style.name == 'chapter':
                label = flowable.getPlainText()
                key = 'chapter-' + label.split('.')[0]
                self.canv.bookmarkPage(key)
                self.canv.addOutlineEntry(label, key, 0)
                self.notify('TOCEntry', (0, label, self.page, key))

    def page_canvas(canvas, doc):
        canvas.setTitle('Футбол Богородск - руководство по продукту')
        canvas.setAuthor('Футбол Богородск')
        canvas.setSubject('source-sha256:' + digest)
        if doc.page == 1:
            return
        canvas.setStrokeColor(colors.HexColor('#D8E3DF'))
        canvas.line(42, height - 31, width - 42, height - 31)
        canvas.setFont('Guide', 8)
        canvas.setFillColor(muted)
        canvas.drawString(42, height - 23, 'ФУТБОЛ БОГОРОДСК / РУКОВОДСТВО ПО ПРОДУКТУ')
        canvas.drawString(42, 23, f'Версия {version} | {date}')
        canvas.drawRightString(width - 42, 23, str(doc.page))

    doc = GuideDoc(str(OUTPUT), pagesize=A4, leftMargin=42, rightMargin=42, topMargin=48, bottomMargin=42)
    doc.addPageTemplates(PageTemplate(id='guide', frames=[Frame(42, 42, width - 84, height - 90, leftPadding=0, rightPadding=0, topPadding=0, bottomPadding=0)], onPage=page_canvas))
    lines = source.splitlines()
    first_chapter = next(i for i, line in enumerate(lines) if line.startswith('## '))
    story = [Spacer(1, 60)]
    cover_title = ParagraphStyle('cover', fontName='GuideBold', fontSize=35, leading=42, textColor=green, spaceAfter=24)
    cover_subtitle = ParagraphStyle('subtitle', fontName='GuideBold', fontSize=22, leading=29, textColor=ink, spaceAfter=32)
    story.append(Paragraph('Футбол<br/>Богородск', cover_title))
    story.append(Paragraph('Паспорт продукта<br/>и руководство по работе с лигой', cover_subtitle))
    for line in lines[4:first_chapter]:
        if line.strip():
            story.append(Paragraph(inline(line), styles['body']))
    story.append(PageBreak())
    story.append(Paragraph('Оглавление', styles['h2']))
    toc = TableOfContents()
    toc.levelStyles = [ParagraphStyle('toc', fontName='Guide', fontSize=10.5, leading=16, textColor=ink, spaceBefore=8, rightIndent=24, alignment=TA_LEFT)]
    story.extend([toc, PageBreak()])
    i = first_chapter
    chapter = 0
    while i < len(lines):
        line = lines[i].strip()
        if line.startswith('## '):
            if chapter:
                story.append(Spacer(1, 24))
            story.append(Paragraph(inline(line[3:]), styles['h1']))
            chapter += 1
        elif line.startswith('### '):
            story.append(Paragraph(inline(line[4:]), styles['h2']))
        elif line.startswith('|'):
            rows = []
            while i < len(lines) and lines[i].strip().startswith('|'):
                cells = [v.strip() for v in lines[i].strip().strip('|').split('|')]
                if not all(re.fullmatch(r':?-+:?', c) for c in cells):
                    rows.append(cells)
                i += 1
            count = len(rows[0])
            available = width - 84
            if count == 5:
                proportions = [.29, .16, .20, .15, .20]
            elif count == 4:
                proportions = [.18, .23, .24, .35]
            else:
                proportions = [1 / count] * count
            cells = [[Paragraph(inline(c), styles['head' if r == 0 else 'cell']) for c in row] for r, row in enumerate(rows)]
            table = LongTable(cells, colWidths=[available * p for p in proportions], repeatRows=1, hAlign='LEFT', spaceBefore=5, spaceAfter=13)
            table.setStyle(TableStyle([
                ('BACKGROUND', (0, 0), (-1, 0), green),
                ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.HexColor('#F0F5F3'), colors.white]),
                ('VALIGN', (0, 0), (-1, -1), 'TOP'),
                ('LEFTPADDING', (0, 0), (-1, -1), 8), ('RIGHTPADDING', (0, 0), (-1, -1), 8),
                ('TOPPADDING', (0, 0), (-1, -1), 8), ('BOTTOMPADDING', (0, 0), (-1, -1), 8),
                ('LINEBELOW', (0, 1), (-1, -1), .4, colors.HexColor('#D8E3DF')),
            ]))
            story.append(table)
            continue
        elif re.match(r'^(\d+\. |- )', line):
            items = []
            while i < len(lines) and re.match(r'^(\d+\. |- )', lines[i].strip()):
                items.append(Paragraph(inline(lines[i].strip()), styles['bullet']))
                i += 1
            while story and isinstance(story[-1], Paragraph) and getattr(story[-1].style, 'keepWithNext', False):
                items.insert(0, story.pop())
            story.append(KeepTogether(items))
            continue
        elif line:
            style = styles['bullet'] if re.match(r'^(\d+\. |- )', line) else styles['body']
            story.append(Paragraph(inline(line), style))
        i += 1
    doc.multiBuild(story)
    print('Создан:', OUTPUT)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true', help='Проверить, что PDF соответствует исходнику')
    args = parser.parse_args()
    source = SOURCE.read_text(encoding='utf-8')
    digest = hashlib.sha256(source.encode('utf-8')).hexdigest()
    if args.check:
        from pypdf import PdfReader
        if not OUTPUT.is_file() or PdfReader(OUTPUT).metadata.subject != 'source-sha256:' + digest:
            raise SystemExit('PDF отсутствует или устарел. Пересоберите руководство командой make product-guide.')
        print('PDF соответствует исходнику:', OUTPUT)
    else:
        build(source, digest)


if __name__ == '__main__':
    main()
