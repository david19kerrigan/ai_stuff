import random
from data import giveData
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from sklearn.linear_model import LinearRegression
from scipy import stats
import pandas as pd
import statsmodels.api as sm
from statsmodels.sandbox.regression.predstd import wls_prediction_std
from statsmodels.iolib.table import (SimpleTable, default_txt_fmt)
import copy
import math
import matplotlib.lines as lines


def Standardization(data, decbond):
    highPoverty = data[data[:, 17] > 0.5]
    state = data[:, 1]
    county = data[:, 2]
    hisp = data[:, 6]
    white = data[:, 7]
    black = data[:, 8]
    asian = data[:, 10]
    poverty = data[:, 17]
    income = data[:, 13]
    unemployment = data[:, -1]
    ind1 = unemployment >= decbond
    ind2 = unemployment < decbond
    unemployment[ind1] = 1
    unemployment[ind2] = 0

    # Y
    outcome = income
    # A
    treatment = unemployment
    # C
    censoring = poverty
    # L
    confounders = np.array([hisp, white, black, asian])
    X1 = sm.add_constant(np.vstack((np.vstack((confounders, treatment)), confounders * treatment)).T)
    model = sm.OLS(outcome, X1)
    result = model.fit()

    second = np.zeros(len(unemployment))
    X1 = sm.add_constant(np.vstack((np.vstack((confounders, second)), confounders * second)).T)
    prediction = result.predict(X1)
    EYlow = np.mean(prediction)

    third = np.array([1.0] * len(unemployment))
    X2 = sm.add_constant(np.vstack((np.vstack((confounders, third)), confounders * third)).T, has_constant='add')
    prediction = result.predict(X2)
    EYhigh = np.mean(prediction)

    causalRatio = EYhigh / EYlow
    causalDifference = EYhigh - EYlow

    return [causalRatio, causalDifference, EYhigh, EYlow]


def bootStrap(boxN, decbond):
    np.set_printoptions(suppress=True,
                        formatter={'float_kind': '{:f}'.format})
    data = giveData()
    allRatio = []
    allDifference = []
    EYhigh = []
    EYlow = []
    for i in range(0, boxN):
        dataBox = []
        for index in range(0, len(data)):
            dataBox.append(data[random.randrange(len(data) - 1)])
        measures = Standardization(np.asarray(dataBox), decbond)
        allRatio.append(measures[0])
        allDifference.append(measures[1])
        EYhigh.append(measures[2])
        EYlow.append(measures[3])
    ratioMean = np.mean(allRatio)
    differenceMean = np.mean(allDifference)
    meanEYhigh = np.mean(EYhigh)
    meanEYhighSTD = np.std(EYhigh)
    meanEYlow = np.mean(EYlow)
    meanEYlowSTD = np.std(EYlow)
    ratioSTD = np.std(allRatio)
    differenceSTD = np.std(allDifference)
    ratioSE = ratioSTD / math.sqrt(len(allRatio))
    differenceSE = differenceSTD / math.sqrt(len(allDifference))
    # print("Causal risk ratio: " , ratioMean , "+/-" , 1.96 * ratioSE)
    # print("Causal risk difference: ", differenceMean, "+/-", 1.96 * differenceSE)
    return [ratioMean, ratioSE, differenceMean, differenceSE, allRatio, allDifference,
            meanEYhigh, meanEYlow, meanEYhighSTD, meanEYlowSTD]


def bootStrap100():
    Y1 = []
    Y2 = []
    X = np.arange(100)
    temp = bootStrap(100, 10.0)
    meanRatio = temp[0]
    meandifference = temp[2]
    Y1 = temp[4]
    Y2 = temp[5]
    plt.figure(figsize=(16, 8))
    plt.subplot(231)
    plt.scatter(X, Y1, s=4)
    plt.plot([0, 100], [meanRatio, meanRatio], color='green')
    plt.subplot(232)
    plt.scatter(X, Y2, s=4)
    plt.plot([0, 100], [meandifference, meandifference], color='green')
    print("0.95 confidence interval for risk ratio is", temp[0], "+/-", 1.96 * temp[1])
    print("0.95 confidence interval for risk difference is", temp[2], "+/-", 1.96 * temp[3])


def bootStrapMultiple():
    Y1 = []
    Y2 = []
    Y3 = []
    Y4 = []
    X = []
    errorRatio = []
    errorDifference = []
    errorHigh = []
    errorLow = []
    for i in range(0, 45):
        temp = bootStrap(5, i + 2)
        Y1.append(temp[0])
        Y2.append(temp[2])
        Y3.append(temp[6])
        Y4.append(temp[7])
        errorRatio.append(temp[1])
        errorDifference.append(temp[3])
        errorHigh.append(temp[8])
        errorLow.append(temp[9])
        X.append(i + 5)

    Y1 = np.asarray(Y1)
    Y2 = np.asarray(Y2)
    Y3 = np.asarray(Y3)
    Y4 = np.asarray(Y4)
    X = np.asarray(X)
    errorRatio = np.asarray(errorRatio)
    errorRatio = 1.96 * errorRatio
    errorDifference = np.asarray(errorDifference)
    errorDifference = 1.96 * errorDifference
    errorHigh = np.asarray(errorHigh)
    errorHigh = 1.96 * errorHigh
    errorLow = np.asarray(errorLow)
    errorLow = 1.96 * errorLow

    fig, (ax0, ax1, ax2) = plt.subplots(figsize=(10, 20), nrows=3, sharex=True)
    ax0.errorbar(X, Y1, yerr=errorRatio, fmt='o', ms=2, mew=4)
    ax0.set_title('Causal Risk Ratio')
    ax1.errorbar(X, Y2, yerr=errorDifference, fmt='o', ms=2, mew=4)
    ax1.set_title('Causal Risk Difference')
    ax2.errorbar(X, Y3, yerr=errorHigh, fmt='o', ms=2, mew=4)
    ax2.errorbar(X, Y4, yerr=errorLow, fmt='o', ms=2, mew=4)
    ax2.set_title('SMO for A = 0, 1')
    plt.show()

