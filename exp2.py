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

  print 'set n Awesomeparallel'
  ns = [2, 4, 8, 16, 32]

  for s in range(1, 5):
    for n in ns:
      cnts_p = []
      for j in range(3):
        raw = check_output([bin, 'AwesomeParallelFirewall', str(s), str(n)])
        cnts_p.append(get_cnt(raw))
      print s, n, average(cnts_p)
