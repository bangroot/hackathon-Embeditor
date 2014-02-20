" Prerequisites
if &compatible || exists('g:loaded_intellij')
  finish
elseif !has('python')
  echohl WarningMsg 
  echomsg 'IntelliJ unavailable: requires Vim with python 2.x support'
  echohl None
  finish
endif

" Boilerplate
let s:save_cpoptions = &cpoptions
set cpoptions&vim

" Initialize python
python << ENDPYTHON
import vim
import sys
sys.path.insert(0, vim.eval('expand("<sfile>:p:h")'))
import idea_vim
ENDPYTHON

" Completion 
function! idea#complete(findstart, base)
  python idea_vim.complete()
endfunction

" Autoactivation

function! idea#autoactivate()
   if !empty(finddir('.idea', './;~'))
       setlocal omnifunc=idea#complete
       nnoremap <buffer> <C-]> :python idea_vim.resolve()<CR>
   endif
endfunction

augroup intellijintegration
    autocmd!
    autocmd BufNewFile,BufReadPost * call idea#autoactivate()
augroup end

" Boilerplate
let g:loaded_intellij = 1
let &cpoptions = s:save_cpoptions
unlet s:save_cpoptions
