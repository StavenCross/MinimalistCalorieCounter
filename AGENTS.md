
# GMER Project Instructions

1. Avoid writing large, God-like files. Files should be capped at 300 lines of code or less unless they are just component/display/ui files. If a file exceeds the line limit, break it up into smaller, more manageable files
1a. As you work through the project, if you come across a file that exceeds the 300 line limit, but is not excluded by the above rule, add to your existing turn to break up the file, and let the user know at the close of the turn, always include regression testing when breaking a large file to ensure the project still works correctly.
2. When writing code, always create a fresh branch, before committing, review your changes and fix any errors. When you push code, merge the branch into main unless instructed otherwise.
3. Use subagents when possible to speed and parallelize development.
4. Be cautious and guarded about code implementation and planning, always review code written at the end of the turn, consider and inform user of implications and risks, this codebase is a critical production product used for real work.
5. Testing is how we ensure we are writing high quality code that doesn't break the project. Write tests for everything, run tests often, iterate your code until tests are green. 
6. Before writing code, always review your skill set and utilize relevant skills to help write better code.
