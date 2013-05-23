Embeditor
=========

The idea behind the project is that many users like code completion and
inspections provided by IntelliJ, but they prefer Vim or Emacs as programmer's
text editors. The only current solution is using Vim emulation in IntelliJ
provided by IdeaVim plugin.

We would like to see more options here:

* Using code completion and inspections from a running instance of IntelliJ in
  a separate Vim process. The main benefit of this approach is native Vim
  experience enhanced by powerful code insight of IntelliJ.

* Embed Vim as a terminal editor in IntelliJ IDE instead of the regular editor
  and provide code completion and inspections there. The point here is
  combination of the full Vim editor with plugins, scripts,
  etc. and the nearly complete set of IntelliJ features: code navigation,
  run configurations and others.

* Run Vim locally and connect to a headless IntelliJ instance on a remote host.
  This would enable, for example, rich code editing in vim over SSH.


Features
--------

None yet.


To-do List
----------

* Terminal window in IntelliJ instead of the editor that runs Vim and
* XML-RPC method in IntelliJ for running inspections for a file
* XML-RPC method in IntelliJ for getting completion variants for an offset in
  the file
* Vim plugin that highlights code based on a list of errors/warnings
* Synchronization of saved file contents between Vim and IntelliJ
* Synchronization of edited file contents between Vim and IntelliJ
* Vim plugin that shows a completion pop-up based on a list of variants


Authors
-------

* traff
* zolotov
* vlan
