import subprocess
from re import search
import os
import sys

def get_cnt(output):
    match = search(r'([0-9]+) pkts', output)
    return int(match.group(1))


def check_output(cmd):
  print 'cmd'
  return subprocess.Popen(cmd, stdout=subprocess.PIPE).communicate()[0]

def average(lst):
  return float(sum(lst))/len(lst)


if __name__ == '__main__':
  bin = "java"

  print 'set n serial Awesomeparallel'
  ns = [2, 4, 8]

  for s in range(4, 9):
    for n in ns:
      cnts_s = []
      cnts_p = []
      for j in range(5):
        print j
        raw = check_output([bin, 'SerialFirewall', str(s), str(n)])
        print 'serial'
        cnts_s.append(get_cnt(raw))
        raw = check_output([bin, 'AwesomeParallelFirewall', str(s), str(n)])
        print 'parallel'
        cnts_p.append(get_cnt(raw))
      print s, n, average(cnts_s), average(cnts_p),
