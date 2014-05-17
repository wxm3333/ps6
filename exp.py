import subprocess
from re import search
import os
import sys

def get_cnt(output):
    match = search(r'([0-9]+) pkts', output)
    return int(match.group(1))


def check_output(cmd):
  return subprocess.Popen(cmd, stdout=subprocess.PIPE).communicate()[0]

def average(lst):
  return float(sum(lst))/len(lst)


if __name__ == '__main__':
  bin = "java"

  print 'set n serial Awesomeparallel'
  ns = [2, 4, 8]

  for s in range(3, 4):
    for n in ns:
      cnts_s = []
      cnts_p = []
      print
      for j in range(3):
        raw = check_output([bin, 'SerialFirewall', str(s), str(n)])
        cnts_s.append(get_cnt(raw))
        raw = check_output([bin, 'AwesomeParallelFirewall', str(s), str(n)])
        cnts_p.append(get_cnt(raw))
      print s, n, average(cnts_s), average(cnts_p),
