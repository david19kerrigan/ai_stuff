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
from statsmodels.graphics.regressionplots import abline_plot
from random import sample

np.set_printoptions(suppress=True,
   formatter={'float_kind':'{:f}'.format})
#this is such a bad way to define constants...
const = {'state':1,'county':2,'hisp':6,'white':7,'black':8,'asian':10,'poverty':17,'income':13,'unemployment':-1}
oldData = giveData()
oldData = oldData[oldData[:,const['poverty']].argsort()]
mean = np.mean(oldData[:,17])
ind1 = oldData[:,17]<mean
ind2 = oldData[:,17]>mean
data = oldData[ind2] 

def scatterTreatment(treatment):
    plt.scatter(data[:,const[treatment]],data[:,const['income']],s=20)
    plt.xlabel(treatment+' - %')
    plt.ylabel('income - USD')
    plt.title(treatment+' vs. income in counties with high poverty')

def getParams(treatmentInput):
    poverty = data[:,17]
    income = data[:,13]
    treatment = data[:,const[treatmentInput]]
    dependantVars = ['black','white','unemployment','hisp','asian']
    dependantVars.remove(treatmentInput)
    confounders = np.array( [ data[:,const[dependantVars[0]]] , data[:,const[dependantVars[1]]] , data[:,const[dependantVars[2]]] , data[:,const[dependantVars[3]]] ] )

    return confounders,treatment,poverty,income

def getWeights(inputTreatment):
    
    confounders,treatment,censoring,outcome = getParams(inputTreatment)

    X = sm.add_constant(np.array(confounders.T))
    model = sm.WLS(treatment,X)
    result = model.fit()
    prediction = result.predict(X)
    prediction=(prediction-np.min(prediction))/np.ptp(prediction)
    prediction[prediction==0] = 0.001
    Wa = 1/prediction  


    X = sm.add_constant(np.array([oldData[:,6],oldData[:,7],oldData[:,8],oldData[:,10],oldData[:,-1]]).T)
    model = sm.WLS(oldData[:,17],X)
    result = model.fit()
    prediction = result.predict()[ind2]
    prediction=(prediction-np.min(prediction))/np.ptp(prediction)
    prediction[prediction==0] = 0.001
    Wc = 1/prediction 

    WcWa = Wc*Wa

    print(inputTreatment+' Wc*Wa weights')
    print(' min   mean    max')
    print('------------------')
    print('{:>04.2f}   {:>04.2f}   {:>04.2f}'.format(
        WcWa.min(),
        WcWa.mean(),
        WcWa.max()
    ))
    print("\n")
    return WcWa

def plotWeightedFit(inputTreatment,WcWa):

    confounders,treatment,censoring,outcome = getParams(inputTreatment)

    X = sm.add_constant(treatment)
    model = sm.WLS(outcome, X, weights=WcWa)
    result = model.fit()
    prediction = result.predict()
    print(result.summary().tables[1])

    stack = np.vstack((prediction,treatment))
    stack = stack.T
    stack = stack[stack[:,1].argsort()]
    mean = np.mean(stack[:,1])
    E1 = np.average(stack[stack[:,1]<mean][:,0]) #low unemployment
    E2 = np.average(stack[stack[:,1]>mean][:,0])  #high unemployment
    difference = E2-E1
    ratio = E2/E1
    diffInterval = abs(result.conf_int()[1][0]-result.params[1])
    ratioInterval = diffInterval/E1

    print("casual risk ratio: " + str(ratio) + " +/- "+str(ratioInterval))
    print("casual risk difference: " + str(difference) + " +/- "+str(diffInterval))

    fig = abline_plot(model_results=result)
    ax = fig.axes[0]
    ax.scatter(X[:,1], outcome,s=WcWa,alpha=0.2,c="red")
    ax.margins(.1)
    fig.set_figheight(3)
    fig.set_figwidth(6)
    plt.xlabel(inputTreatment+' - %')
    plt.ylabel('income - USD')
    plt.title('IP Weighted '+inputTreatment+' vs. income in counties with high poverty')
    plt.show()
    
    return {'ratio':ratio,'difference':difference,'ratioConf':ratioInterval,'differenceConf':diffInterval}

def plotRobustSample(inputTreatment,WcWa):
    confounders,treatment,censoring,outcome = getParams(inputTreatment)
    samp = sample(range(len(outcome)),10000)
    treatment = treatment[samp]
    outcome = outcome[samp]
    X = sm.add_constant(treatment)
    model = sm.WLS(outcome, X, weights=WcWa[samp])
    result = model.fit(cov_type="HC3")
    prediction = result.predict()
    print(result.summary().tables[1])

    stack = np.vstack((prediction,treatment))
    stack = stack.T
    stack = stack[stack[:,1].argsort()]
    mean = np.mean(stack[:,1])
    E1 = np.average(stack[stack[:,1]<mean][:,0]) #low unemployment
    E2 = np.average(stack[stack[:,1]>mean][:,0])  #high unemployment
    difference = E2-E1
    ratio = E2/E1
    diffInterval = abs(result.conf_int()[1][0]-result.params[1])
    ratioInterval = diffInterval/E1
    
    print("casual risk ratio: " + str(ratio) + " +/- "+str(ratioInterval))
    print("casual risk difference: " + str(difference) + " +/- "+str(diffInterval))

    fig = abline_plot(model_results=result)
    ax = fig.axes[0]
    ax.scatter(X[:,1], outcome,s=WcWa,alpha=0.2,c="red")
    ax.margins(.1)
    fig.set_figheight(3)
    fig.set_figwidth(6)
    plt.xlabel(inputTreatment+' - %')
    plt.ylabel('income - USD')
    plt.title('(Robust) IP Weighted '+inputTreatment+' vs. income in counties with high poverty')
    plt.show()

    return {'ratio':ratio,'difference':difference,'ratioConf':ratioInterval,'differenceConf':diffInterval}

def resultGraphs(ipResults, standResults):
    fig = plt.figure()
    fig.set_figheight(15)
    fig.set_figwidth(25)

    barWidth = 0.25

    means1 = [ipResults['difference']]
    means2 = [standResults['difference']]

    plt.subplot(1,2,1)

    r1 = np.arange(len(means2))
    r2 = [x + barWidth for x in r1]

    y_r1 = abs(ipResults['differenceConf'])
    y_r2 = abs(standResults['differenceConf'])
    plt.bar(r2, means2, color='blue', width=barWidth, edgecolor='white', label='Standardization', yerr = y_r2,alpha=0.2)
    plt.bar(r1, means1, color='red', width=barWidth, edgecolor='white', label='IP Weighting', yerr = y_r1,alpha=0.2)

    plt.xlabel('Causal Measurements of Unemployment on Income', fontweight='bold')
    plt.xticks([r + barWidth/2 for r in range(len(means1))], ['Causal Difference'])
    plt.legend()
    plt.ylim(-6000,-21000)
    means1 = [ipResults['ratio']]
    means2 = [standResults['ratio']]

    plt.subplot(1,2,2)
    r1 = np.arange(len(means2))
    r2 = [x + barWidth for x in r1]

    y_r1 = abs(ipResults['ratioConf'])
    y_r2 = abs(standResults['ratioConf'])
    plt.bar(r2, means2, color='blue', width=barWidth, edgecolor='white', label='Standardization', yerr = y_r2,alpha=0.2)
    plt.bar(r1, means1, color='red', width=barWidth, edgecolor='white', label='IP Weighting', yerr = y_r1,alpha=0.2)

    plt.xlabel('Causal Measurements of Unemployment on Income', fontweight='bold')
    plt.xticks([r + barWidth/2 for r in range(len(means1))], ['Causal Ratio'])
    plt.legend()
    plt.ylim(0.75,0.831)
    plt.show()