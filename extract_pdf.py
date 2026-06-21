import sys
sys.stdout.reconfigure(encoding='utf-8')
from pypdf import PdfReader

r = PdfReader(r'c:\Users\tvida\Downloads\files\Strategyproof Mechanism for Two-Sided Matching with Resource Allocation.pdf')
print(f'Pages: {len(r.pages)}')
for i, p in enumerate(r.pages):
    text = p.extract_text()
    print(f'--- PAGE {i+1} ---')
    print(text)
