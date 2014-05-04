/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.moorestudio.seniorimageprocessing;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author nrwebb
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement()
public class SeniorSorterData {
    
    @XmlAttribute
    private int nextImageIndex;
    
    @XmlAttribute
    private int numImageIndexDigits;
    
    public SeniorSorterData()
    {
        nextImageIndex = 0;
        numImageIndexDigits = 4;
    }

    public int getNextImageIndex() {
        return nextImageIndex;
    }
    
    public int getNumImageIndexDigits()
    {
        return numImageIndexDigits;
    }
    
    public int pollNextImageIndex()
    {
        return nextImageIndex++;
    }
    
}
