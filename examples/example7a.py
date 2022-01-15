# 7
# Right
def sub(x, y):
    return x - y


def sub(x, y, z):
    return x - y - z


# Wrong
def add(x, y):
    print x


def add(x, y, z=1):
    print x
