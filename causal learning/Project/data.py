import pandas as pd
import numpy as np

def standardizeDiscrete(values):
    domain = []
    discreteArray=[]
    for i in values:
        if i not in domain:
            domain.append(i)
    for i in values:
        discreteArray.append(domain.index(i))
    return discreteArray

def giveData():
    df = pd.read_csv('census.csv', sep=',')
    df.County = standardizeDiscrete(df.County)
    df.State = standardizeDiscrete(df.State)
    df = df.dropna()
    nparray = df.to_numpy()
    return nparray




