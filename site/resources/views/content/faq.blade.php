@extends('layouts.default')
@section('title', $data->FqL_title . ' - ' . trans('common.title'))
@section('site-description', $data->FqL_title)
@section('site-keywords', trans('page-keywords'))
{{-- @section('site-canonical', $canonical) --}}
{{-- @section('page-hreflang', $canonical) --}}
@section('article-headline', $data->FqL_title)
{{-- {{-- @section('article-date', $publishDate) --}}
@section('article-modification-date', $data->created_at)
@section('article-description', trans('page-description'))
@section('article-tag', '')


@section('content')
    @include('layouts.breadcrumb')

    <div class="row">
        <div class="col ">
            <h1 class="t-h1">{{ trans('common.box_FAQ') }}</h1>
        </div>
    </div>

    {{-- {{ddd($nav["data"])}} --}}
    <div class=" mb-5 m-article">

        <div class="row m-info">
            <div class="col-sm-12 col-xs-12 col-xl-9 col-lg-9 col-md-8">

                <h2 class="">{{ $data->FqL_title }}</h2>
                <div class="">
                    {!! $data->FqL_body !!}
                </div>


            </div>
            <div class="col-sm-12 col-xs-12 col-xl-3 col-lg-3 col-md-4">
                <div class="m-faq">
                    @include('content.faq_box')
                </div>
            </div>





        </div>
    </div>





@stop
