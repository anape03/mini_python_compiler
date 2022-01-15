# 4
# Right
def add(x, y=2):
   return x + y
k=3
print add(2, k)
print add('hi', 'hey')

# Wrong
def mul(x, y=2):
   return x * y
k="hello world"
print mul("hi", k)
