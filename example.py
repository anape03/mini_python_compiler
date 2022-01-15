#! /usr/bin/env python
# A miniPython example

# 1
# def add(x,y):
#    return x + y
# print k

# def add(x,y):
#    return x + y
# print k
# k =0

# 2

# 3
# def add(x,y=2):
#    return x + y
# print add(1)

# 4
# x="hello world"
# print x+2

# def add(x, y=2):
#    print x + y
# # k="hello world"
# k = 3
# print add(2, k)
# # print add('hi', 'hey')

# 5
# print 3 + None
# x = 1
# print x - None
# print 2 ** 3 / None
# print None * None

# 6
# def add(x,y):
#    return "hello world"
# print add(2,1)+2

# 7
# def add(x, y):
#     print x


# def add(x, y, z=1):
#     print x

# *****************

# def add(x, y, z):
#     print x


# def add(x, y, z=1):
#     print x

# *****************

# def add(x, y):
#     print x


# def add(x, y, z):
#     print x


# def add(x, y):
#     print y

# EXTREME TEST
def add(x, y):
    return x + y

print add(add(1, 2) + add('hi', 'hey'))
