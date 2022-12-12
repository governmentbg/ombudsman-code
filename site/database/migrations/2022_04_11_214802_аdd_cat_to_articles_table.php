<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class АddCatToArticlesTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::table('m_article', function (Blueprint $table) {
            $table->integer('Cat_id')->comment('Category ID')->after('Ar_parent_id')->nullable()->unsigned();

            $table->foreign('Cat_id')->references('Cat_id')->on('m_category');
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::table('m_article', function (Blueprint $table) {
            //
        });
    }
}
