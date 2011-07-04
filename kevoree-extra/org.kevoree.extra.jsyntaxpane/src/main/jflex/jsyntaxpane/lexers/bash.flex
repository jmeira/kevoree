/*
 * Copyright 2008 Ayman Al-Sairafi ayman.alsairafi@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License
 *       at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jsyntaxpane.lexers;


import jsyntaxpane.Token;
import jsyntaxpane.TokenTypes;

%%

%public
%class BashLexer
%extends DefaultJFlexLexer
%final
%unicode
%char
%type Token

%{
    /**
     * Create an empty lexer, yyrset will be called later to reset and assign
     * the reader
     */
    public BashLexer() {
        super();
    }

    private static final byte PARAN     = 1;
    private static final byte BRACKET   = 2;
    private static final byte CURLY     = 3;
    private static final byte DO        = 4;
    private static final byte CASE      = 5;
    private static final byte IF        = 5;
    private static final byte INT_EXPR  = 6;

    @Override
    public int yychar() {
        return yychar;
    }

%}

LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]

Identifier = [a-zA-Z][a-zA-Z0-9_]*

Comment = "#"  {InputCharacter}* {LineTerminator}?
Shebang = "#!" {InputCharacter}* {LineTerminator}?

StringCharacter = [^\r\n\"\\]
SingleCharacter = [^\r\n\'\\]
BackQuoteChars  = [^\r\n\`\\]


%%

<YYINITIAL> 
{
  /* Bash keywords */
  "if"                          { return token(TokenTypes.KEYWORD,  IF); }
  "fi"                          { return token(TokenTypes.KEYWORD, -IF); }
  "do"                          { return token(TokenTypes.KEYWORD,  DO); }
  "done"                        { return token(TokenTypes.KEYWORD, -DO); }
  "case"                        { return token(TokenTypes.KEYWORD,  CASE); }
  "esac"                        { return token(TokenTypes.KEYWORD, -CASE); }
  "$(("                         { return token(TokenTypes.KEYWORD,  INT_EXPR); }
  "))"                          { return token(TokenTypes.KEYWORD, -INT_EXPR); }

  "("                           { return token(TokenTypes.OPERATOR,  PARAN); }
  ")"                           { return token(TokenTypes.OPERATOR, -PARAN); }
  "{"                           { return token(TokenTypes.OPERATOR,  CURLY); }
  "}"                           { return token(TokenTypes.OPERATOR, -CURLY); }
  "["                           { return token(TokenTypes.OPERATOR,  BRACKET); }
  "]"                           { return token(TokenTypes.OPERATOR, -BRACKET); }

  "-eq"                         |
  "-ne"                         |
  "-lt"                         |
  "-gt"                         |
  "-ge"                         |
  "-le"                         |
  ">="                          |
  "<="                          |
  "=="                          |
  "!="                          |
  "-z"                          |
  "-n"                          |
  "=~"                          |

  "$"                           |
  "#"                           |
  "&"                           |
  "."                           |
  ";"                           |
  "+"                           |
  "-"                           |
  "="                           |
  "/"                           |
  "++"                          |
  "@"                           { return token(TokenTypes.OPERATOR); }

  "then"                        |
  "else"                        |
  "elif"                        |
  "for"                         |
  "in"                          |
  "until"                       |
  "while"                       |
  "break"                       |
  "local"                       |
  "continue"                    { return token(TokenTypes.KEYWORD); }

  /* string literal */
  \"{StringCharacter}+\"        |

  \'{SingleCharacter}+\         { return token(TokenTypes.STRING); }

  \`{BackQuoteChars}+\`         { return token(TokenTypes.STRING2); }


  /* Other commands */
  "alias"                    |
  "apropos"                  |
  "apt"                      |
  "aspell"                   |
  "awk"                      |
  "bash"                     |
  "basename"                 |
  "bc"                       |
  "bg"                       |
  "builtin"                  |
  "bzip2"                    |
  "cal"                      |
  "cat"                      |
  "cd"                       |
  "cfdisk"                   |
  "chgrp"                    |
  "chmod"                    |
  "chown"                    |
  "chroot"                   |
  "chkconfig"                |
  "cksum"                    |
  "clear"                    |
  "cmp"                      |
  "comm"                     |
  "command"                  |
  "continue"                 |
  "cp"                       |
  "cron"                     |
  "crontab"                  |
  "csplit"                   |
  "cut"                      |
  "date"                     |
  "dc"                       |
  "dd"                       |
  "ddrescue"                 |
  "declare"                  |
  "df"                       |
  "diff"                     |
  "diff3"                    |
  "dig"                      |
  "dir"                      |
  "dircolors"                |
  "dirname"                  |
  "dirs"                     |
  "dmesg"                    |
  "du"                       |
  "echo"                     |
  "egrep"                    |
  "eject"                    |
  "enable"                   |
  "env"                      |
  "ethtool"                  |
  "eval"                     |
  "exec"                     |
  "exit"                     |
  "expect"                   |
  "expand"                   |
  "export"                   |
  "expr"                     |
  "false"                    |
  "fdformat"                 |
  "fdisk"                    |
  "fg"                       |
  "fgrep"                    |
  "file"                     |
  "find"                     |
  "fmt"                      |
  "fold"                     |
  "format"                   |
  "free"                     |
  "fsck"                     |
  "ftp"                      |
  "function"                 |
  "gawk"                     |
  "getopts"                  |
  "grep"                     |
  "groups"                   |
  "gzip"                     |
  "hash"                     |
  "head"                     |
  "history"                  |
  "hostname"                 |
  "id"                       |
  "ifconfig"                 |
  "ifdown"                   |
  "ifup"                     |
  "import"                   |
  "install"                  |
  "join"                     |
  "kill"                     |
  "killall"                  |
  "less"                     |
  "let"                      |
  "ln"                       |
  "locate"                   |
  "logname"                  |
  "logout"                   |
  "look"                     |
  "lpc"                      |
  "lpr"                      |
  "lprint"                   |
  "lprintd"                  |
  "lprintq"                  |
  "lprm"                     |
  "ls"                       |
  "lsof"                     |
  "man"                      |
  "mkdir"                    |
  "mkfifo"                   |
  "mkisofs"                  |
  "mknod"                    |
  "more"                     |
  "mount"                    |
  "mtools"                   |
  "mv"                       |
  "mmv"                      |
  "netstat"                  |
  "nice"                     |
  "nl"                       |
  "nohup"                    |
  "nslookup"                 |
  "open"                     |
  "op"                       |
  "passwd"                   |
  "paste"                    |
  "pathchk"                  |
  "ping"                     |
  "popd"                     |
  "pr"                       |
  "printcap"                 |
  "printenv"                 |
  "printf"                   |
  "ps"                       |
  "pushd"                    |
  "pwd"                      |
  "quota"                    |
  "quotacheck"               |
  "quotactl"                 |
  "ram"                      |
  "rcp"                      |
  "read"                     |
  "readonly"                 |
  "reboot"                   |
  "renice"                   |
  "remsync"                  |
  "return"                   |
  "rev"                      |
  "rm"                       |
  "rmdir"                    |
  "rsync"                    |
  "screen"                   |
  "scp"                      |
  "sdiff"                    |
  "sed"                      |
  "select"                   |
  "seq"                      |
  "set"                      |
  "sftp"                     |
  "shift"                    |
  "shopt"                    |
  "shutdown"                 |
  "sleep"                    |
  "slocate"                  |
  "sort"                     |
  "source"                   |
  "split"                    |
  "ssh"                      |
  "strace"                   |
  "su"                       |
  "sudo"                     |
  "sum"                      |
  "symlink"                  |
  "sync"                     |
  "tail"                     |
  "tar"                      |
  "tee"                      |
  "test"                     |
  "time"                     |
  "times"                    |
  "touch"                    |
  "top"                      |
  "traceroute"               |
  "trap"                     |
  "tr"                       |
  "true"                     |
  "tsort"                    |
  "tty"                      |
  "type"                     |
  "ulimit"                   |
  "umask"                    |
  "umount"                   |
  "unalias"                  |
  "uname"                    |
  "unexpand"                 |
  "uniq"                     |
  "units"                    |
  "unset"                    |
  "unshar"                   |
  "useradd"                  |
  "usermod"                  |
  "users"                    |
  "uuencode"                 |
  "uudecode"                 |
  "v"                        |
  "vdir"                     |
  "vi"                       |
  "vmstat"                   |
  "watch"                    |
  "wc"                       |
  "whereis"                  |
  "which"                    |
  "who"                      |
  "whoami"                   |
  "Wget"                     |
  "write"                    |
  "xargs"                    |
  "yes"                      { return token(TokenTypes.KEYWORD); }

  {Identifier}               { return token(TokenTypes.IDENTIFIER); }

  /* labels */
  ":" [a-zA-Z][a-zA-Z0-9_]*  { return token(TokenTypes.TYPE); }

  /* comments */
  {Shebang}                   { return token(TokenTypes.COMMENT2); }
  {Comment}                   { return token(TokenTypes.COMMENT); }
  . | {LineTerminator}        { /* skip */ }

}

<<EOF>>                          { return null; }